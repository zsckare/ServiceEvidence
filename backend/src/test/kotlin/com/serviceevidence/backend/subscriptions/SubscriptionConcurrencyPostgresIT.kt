package com.serviceevidence.backend.subscriptions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * EN: Two independent transactions compete for the final subscription slot.
 * ES: Dos transacciones independientes compiten por el último cupo.
 * IMPORTANT: Do not annotate this test with @Transactional: workers need separate commits.
 */
@SpringBootTest
@ActiveProfiles("integration")
class SubscriptionConcurrencyPostgresIT {
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var quotas: SubscriptionQuotaService
    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    fun guardDatabase() {
        val database = jdbc.queryForObject("SELECT current_database()", String::class.java)
        require(database?.endsWith("_test") == true) {
            "Refusing to modify database '$database'; expected a dedicated *_test database"
        }
    }

    @Test
    fun `simultaneous membership creations cannot exceed Starter limit`() {
        val tenant = UUID.randomUUID()
        val userIds = List(6) { UUID.randomUUID() }
        val membershipIds = List(6) { UUID.randomUUID() }
        val tx = TransactionTemplate(transactionManager)
        try {
            jdbc.update("INSERT INTO tenants(id,code,name) VALUES (?,?,?)", tenant, "concurrency-$tenant", "Concurrency test")
            jdbc.update("INSERT INTO tenant_subscriptions(tenant_id,plan_code) VALUES (?, 'STARTER')", tenant)
            userIds.forEach { user ->
                jdbc.update("INSERT INTO platform_users(id,email,password_hash,full_name) VALUES (?,?,?,?)", user, "$user@example.invalid", "test-only", "Concurrent test")
            }
            repeat(4) { index ->
                jdbc.update("INSERT INTO tenant_memberships(id,tenant_id,user_id,role,enabled) VALUES (?,?,?,'TECHNICIAN',TRUE)",
                    membershipIds[index], tenant, userIds[index])
            }

            val start = CountDownLatch(1)
            val ready = CountDownLatch(2)
            val executor = Executors.newFixedThreadPool(2)
            try {
                val results = (4..5).map { index ->
                    executor.submit<Boolean> {
                        ready.countDown()
                        check(start.await(15, TimeUnit.SECONDS))
                        try {
                            tx.executeWithoutResult {
                                quotas.requireUserSlot(tenant)
                                jdbc.update("INSERT INTO tenant_memberships(id,tenant_id,user_id,role,enabled) VALUES (?,?,?,'TECHNICIAN',TRUE)",
                                    membershipIds[index], tenant, userIds[index])
                            }
                            true
                        } catch (ex: ResponseStatusException) {
                            assertEquals(HttpStatus.CONFLICT, ex.statusCode)
                            false
                        }
                    }
                }
                assertTrue(ready.await(15, TimeUnit.SECONDS), "Workers did not become ready")
                start.countDown()
                assertEquals(1, results.count { it.get(30, TimeUnit.SECONDS) }, "Exactly one worker must obtain the final slot")
                assertEquals(5L, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM tenant_memberships WHERE tenant_id=? AND enabled=TRUE", Long::class.java, tenant))
            } finally {
                start.countDown()
                executor.shutdownNow()
            }
        } finally {
            // EN: Cleanup is explicit because worker transactions must commit independently.
            // ES: Limpieza explícita porque las transacciones concurrentes confirman sus cambios.
            jdbc.update("DELETE FROM tenant_memberships WHERE tenant_id=?", tenant)
            jdbc.update("DELETE FROM tenant_subscriptions WHERE tenant_id=?", tenant)
            jdbc.update("DELETE FROM tenants WHERE id=?", tenant)
            userIds.forEach { jdbc.update("DELETE FROM platform_users WHERE id=?", it) }
        }
    }
}
