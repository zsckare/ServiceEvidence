package com.serviceevidence.backend.billing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/** EN: Financial aggregates and method authorization with real PostgreSQL.
 * ES: Agregados financieros y autorización de métodos con PostgreSQL real.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class BillingMetricsPostgresIT {
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var metrics: BillingMetricsController

    private fun tenant(): UUID {
        val database = jdbc.queryForObject("SELECT current_database()", String::class.java)
        require(database?.endsWith("_test") == true) { "Dedicated *_test database required" }
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO tenants(id,code,name) VALUES(?,?,?)", id, "finance-$id", "Finance test")
        jdbc.update("INSERT INTO tenant_subscriptions(tenant_id,plan_code) VALUES(?,'STARTER')", id)
        return id
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun `aggregates invoice balances and recorded payments by currency`() {
        val tenantId = tenant()
        val invoiceId = UUID.randomUUID()
        jdbc.update("""INSERT INTO billing_invoices(id,tenant_id,invoice_number,plan_code,currency,amount_minor,paid_amount_minor,status,due_at)
            VALUES(?, ?, ?, 'STARTER','MXN',1000,400,'PARTIALLY_PAID',CURRENT_TIMESTAMP - INTERVAL '1 day')""",
            invoiceId, tenantId, "FIN-$invoiceId")
        jdbc.update("""INSERT INTO billing_payments(id,invoice_id,tenant_id,amount_minor,currency,method)
            VALUES(?,?,?,400,'MXN','CASH')""", UUID.randomUUID(), invoiceId, tenantId)
        val summary = metrics.overview(LocalDate.now().minusDays(1), LocalDate.now())
        val mxn = summary.currencies.first { it.currency == "MXN" }
        assertTrue(mxn.collectedMinor >= 400)
        assertTrue(mxn.invoicedMinor >= 1000)
        assertTrue(mxn.outstandingMinor >= 600)
        assertTrue(mxn.overdueMinor >= 600)
        assertTrue(summary.subscriptions.active >= 0)
    }

    @Test
    @WithMockUser(roles = ["TECHNICIAN"])
    fun `technician cannot access platform financial metrics`() {
        assertThrows(AccessDeniedException::class.java) {
            metrics.overview(LocalDate.now().minusDays(1), LocalDate.now())
        }
    }
}
