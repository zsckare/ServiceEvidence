package com.serviceevidence.backend.subscriptions

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * EN: Real PostgreSQL quota tests. Data is rolled back after each test.
 * ES: Pruebas reales de cuotas con PostgreSQL. Cada prueba revierte sus datos.
 *
 * Run against a dedicated database whose name ends in _test.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@Tag("postgres-integration")
class SubscriptionQuotaPostgresIT {
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var quotas: SubscriptionQuotaService

    @BeforeEach
    fun ensureDedicatedDatabase() {
        val name = jdbc.queryForObject("SELECT current_database()", String::class.java)
        require(name?.endsWith("_test") == true) {
            "Refusing integration test on database '$name'; use a dedicated *_test database"
        }
    }

    private fun tenant(plan: String = "STARTER"): UUID {
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO tenants(id,code,name) VALUES (?,?,?)", id, "test-${id}", "Integration test")
        jdbc.update("INSERT INTO tenant_subscriptions(tenant_id,plan_code) VALUES (?,?)", id, plan)
        return id
    }

    private fun membership(tenantId: UUID, enabled: Boolean = true): UUID {
        val userId = UUID.randomUUID()
        jdbc.update("INSERT INTO platform_users(id,email,password_hash,full_name) VALUES (?,?,?,?)",
            userId, "$userId@example.invalid", "test-only", "Test User")
        val membershipId = UUID.randomUUID()
        jdbc.update("INSERT INTO tenant_memberships(id,tenant_id,user_id,role,enabled) VALUES (?,?,?,?,?)",
            membershipId, tenantId, userId, "TECHNICIAN", enabled)
        return membershipId
    }

    @Test
    fun `user quota counts active memberships for the selected tenant only`() {
        val first = tenant()
        val second = tenant()
        repeat(5) { membership(first) }
        repeat(5) { membership(second) }
        val error = assertThrows(ResponseStatusException::class.java) { quotas.requireUserSlot(first) }
        assertEquals(HttpStatus.CONFLICT, error.statusCode)
        val membershipId = jdbc.queryForObject(
            "SELECT id FROM tenant_memberships WHERE tenant_id=? LIMIT 1", UUID::class.java, first)!!
        jdbc.update("UPDATE tenant_memberships SET enabled=FALSE WHERE id=?", membershipId)
        assertDoesNotThrow { quotas.requireUserSlot(first) }
        // EN: A different tenant's membership count must not consume this tenant's quota.
        // ES: Las membresías de otro tenant no consumen la cuota del primero.
        val third = tenant()
        assertDoesNotThrow { quotas.requireUserSlot(third) }
    }

    @Test
    fun `work order quota uses only current month and selected tenant`() {
        val first = tenant()
        val second = tenant()
        val author = UUID.randomUUID()
        jdbc.update("INSERT INTO platform_users(id,email,password_hash,full_name) VALUES (?,?,?,?)",
            author, "$author@example.invalid", "test-only", "Author")
        fun insertOrder(tenantId: UUID, monthsAgo: Int = 0) {
            val customerId = UUID.randomUUID()
            val locationId = UUID.randomUUID()
            jdbc.update("INSERT INTO customers(id,tenant_id,name) VALUES (?,?,?)", customerId, tenantId, "Customer")
            jdbc.update("""INSERT INTO service_locations(id,tenant_id,customer_id,name,address_line1,city,country)
                VALUES (?,?,?,?,?,?,?)""", locationId, tenantId, customerId, "Location", "Street", "City", "MX")
            jdbc.update("""INSERT INTO work_orders(id,tenant_id,customer_id,location_id,title,created_by,created_at)
                VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP - (? * INTERVAL '1 month'))""",
                UUID.randomUUID(), tenantId, customerId, locationId, "Test order", author, monthsAgo)
        }
        repeat(100) { insertOrder(first) }
        insertOrder(second)
        insertOrder(first, 2)
        val error = assertThrows(ResponseStatusException::class.java) { quotas.requireWorkOrderSlot(first) }
        assertEquals(HttpStatus.CONFLICT, error.statusCode)
        assertDoesNotThrow { quotas.requireWorkOrderSlot(second) }
    }

    @Test
    fun `unknown tenant is rejected before checking subscription`() {
        val error = assertThrows(ResponseStatusException::class.java) {
            quotas.requireUserSlot(UUID.randomUUID())
        }
        assertEquals(HttpStatus.NOT_FOUND, error.statusCode)
    }
}
