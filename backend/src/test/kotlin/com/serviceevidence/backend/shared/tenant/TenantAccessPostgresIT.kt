package com.serviceevidence.backend.shared.tenant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/** EN: Tenant membership and role authorization against PostgreSQL.
 * ES: Autorización por membresía y rol contra PostgreSQL real.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class TenantAccessPostgresIT {
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var access: TenantAccess

    private fun jwt(userId: UUID, tenantId: UUID): Jwt = Jwt.withTokenValue("test-token")
        .header("alg", "HS256")
        .subject(userId.toString())
        .claim("tenant_id", tenantId.toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build()

    @Test
    fun `tenant membership cannot authorize access to another tenant`() {
        guardDatabase()
        val first = tenant()
        val second = tenant()
        val user = user()
        membership(first, user, "TENANT_ADMIN")
        assertEquals(first, access.require(jwt(user, first), "TENANT_ADMIN"))
        val error = assertThrows(ResponseStatusException::class.java) {
            access.require(jwt(user, second), "TENANT_ADMIN")
        }
        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
    }

    @Test
    fun `technician cannot perform tenant admin actions`() {
        guardDatabase()
        val tenant = tenant()
        val user = user()
        membership(tenant, user, "TECHNICIAN")
        assertEquals(tenant, access.require(jwt(user, tenant), "TENANT_ADMIN", "TECHNICIAN"))
        val error = assertThrows(ResponseStatusException::class.java) {
            access.require(jwt(user, tenant), "TENANT_ADMIN")
        }
        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
    }

    @Test
    fun `disabled membership cannot access tenant`() {
        guardDatabase()
        val tenant = tenant()
        val user = user()
        membership(tenant, user, "TENANT_ADMIN", false)
        val error = assertThrows(ResponseStatusException::class.java) {
            access.require(jwt(user, tenant), "TENANT_ADMIN")
        }
        assertEquals(HttpStatus.FORBIDDEN, error.statusCode)
    }

    private fun guardDatabase() {
        val database = jdbc.queryForObject("SELECT current_database()", String::class.java)
        require(database?.endsWith("_test") == true) { "Dedicated *_test database required, got '$database'" }
    }
    private fun tenant(): UUID = UUID.randomUUID().also {
        jdbc.update("INSERT INTO tenants(id,code,name) VALUES (?,?,?)", it, "auth-$it", "Auth test")
    }
    private fun user(): UUID = UUID.randomUUID().also {
        jdbc.update("INSERT INTO platform_users(id,email,password_hash,full_name) VALUES (?,?,?,?)", it, "$it@example.invalid", "test-only", "Auth test")
    }
    private fun membership(tenant: UUID, user: UUID, role: String, enabled: Boolean = true) {
        jdbc.update("INSERT INTO tenant_memberships(id,tenant_id,user_id,role,enabled) VALUES (?,?,?,?,?)",
            UUID.randomUUID(), tenant, user, role, enabled)
    }
}
