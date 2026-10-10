package com.serviceevidence.backend.billing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/** EN: Real PostgreSQL billing integration tests.
 * ES: Pruebas de facturación contra PostgreSQL real.
 */
@SpringBootTest
@ActiveProfiles("integration")
@WithMockUser(username = "billing-integration-admin", roles = ["SUPER_ADMIN"])
@Transactional
class BillingAutomationPostgresIT {
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var billing: BillingController
    @Autowired lateinit var renewals: BillingRenewalService

    private lateinit var tenant: UUID
    // EN: Match the JWT subject with a real database user for FK integrity.
    // ES: El subject del JWT debe corresponder a un usuario real en PostgreSQL.
    private val platformUserId = UUID.randomUUID()
    private val jwt: Jwt = Jwt.withTokenValue("test")
        .header("alg", "none").subject(platformUserId.toString())
        .build()

    @BeforeEach
    fun setup() {
        val database = jdbc.queryForObject("SELECT current_database()", String::class.java)
        require(database?.endsWith("_test") == true) { "Billing tests require a dedicated *_test database" }
        // EN: Insert the authenticated actor before billing writes reference it.
        // ES: Crear el usuario antes de que los pagos lo referencien.
        jdbc.update(
            """INSERT INTO platform_users(id,email,password_hash,full_name,platform_admin)
               VALUES(?,?,?,?,TRUE)""",
            platformUserId, "billing-test-$platformUserId@example.com",
            "integration-test-placeholder", "Billing Integration Admin"
        )
        tenant = UUID.randomUUID()
        jdbc.update("INSERT INTO tenants(id,code,name) VALUES(?,?,?)", tenant, "billing-$tenant", "Billing test")
        jdbc.update("INSERT INTO tenant_subscriptions(tenant_id,plan_code) VALUES(?,'STARTER')", tenant)
    }

    private fun invoice(amount: Long = 1000L): UUID {
        val id = UUID.randomUUID()
        jdbc.update("""INSERT INTO billing_invoices(id,tenant_id,invoice_number,plan_code,currency,amount_minor,due_at)
            VALUES(?,? ,?,'STARTER','MXN',?,CURRENT_TIMESTAMP + INTERVAL '7 days')""",
            id, tenant, "TEST-$id", amount)
        return id
    }

    @Test
    fun `partial payment updates balance and final payment settles invoice`() {
        val id = invoice()
        billing.recordPayment(tenant, id, RecordPaymentRequest("CASH", amountMinor = 400), jwt)
        val partial = billing.platformInvoices(tenant).first { it.id == id }
        assertEquals("PARTIALLY_PAID", partial.status)
        assertEquals(400L, partial.paidAmountMinor)
        assertEquals(600L, partial.balanceMinor)
        billing.recordPayment(tenant, id, RecordPaymentRequest("BANK_TRANSFER", amountMinor = 600), jwt)
        val settled = billing.platformInvoices(tenant).first { it.id == id }
        assertEquals("PAID", settled.status)
        assertEquals(0L, settled.balanceMinor)
        assertNotNull(settled.paidAt)
    }

    @Test
    fun `idempotent payment cannot be recorded twice`() {
        val id = invoice()
        val body = RecordPaymentRequest("CASH", "ref-1", 300, "unique-1")
        val first = billing.recordPayment(tenant, id, body, jwt)
        val replay = billing.recordPayment(tenant, id, body, jwt)
        assertEquals(first.id, replay.id)
        assertEquals(1L, jdbc.queryForObject(
            "SELECT COUNT(*) FROM billing_payments WHERE invoice_id=?", Long::class.java, id))
        assertThrows(ResponseStatusException::class.java) {
            billing.recordPayment(tenant, id, body.copy(amountMinor = 500), jwt)
        }
    }

    @Test
    fun `payment cannot exceed remaining balance`() {
        val id = invoice()
        assertThrows(ResponseStatusException::class.java) {
            billing.recordPayment(tenant, id, RecordPaymentRequest("CASH", amountMinor = 1001), jwt)
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM billing_payments WHERE invoice_id=?", Long::class.java, id))
    }

    @Test
    fun `expired subscription without paid invoice becomes past due`() {
        jdbc.update("""UPDATE tenant_subscriptions SET current_period_start=CURRENT_TIMESTAMP-INTERVAL '40 days',
            current_period_end=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE tenant_id=?""", tenant)
        renewals.reconcileTenant(tenant)
        assertEquals("PAST_DUE", billing.platformSubscription(tenant).status)
    }

    @Test
    fun `expired subscription with paid invoice renews once`() {
        jdbc.update("""UPDATE tenant_subscriptions SET current_period_start=CURRENT_TIMESTAMP-INTERVAL '40 days',
            current_period_end=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE tenant_id=?""", tenant)
        val id = invoice()
        jdbc.update("""UPDATE billing_invoices SET status='PAID',paid_amount_minor=amount_minor,
            paid_at=CURRENT_TIMESTAMP-INTERVAL '2 days',issued_at=CURRENT_TIMESTAMP-INTERVAL '3 days' WHERE id=?""", id)
        val before = billing.platformSubscription(tenant).periodEnd
        renewals.reconcileTenant(tenant)
        val after = billing.platformSubscription(tenant)
        assertEquals("ACTIVE", after.status)
        assertTrue(after.periodEnd.isAfter(before))
        renewals.reconcileTenant(tenant)
        assertEquals(after.periodEnd, billing.platformSubscription(tenant).periodEnd)
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM billing_renewal_runs WHERE tenant_id=?", Long::class.java, tenant))
    }

    @Test
    fun `scheduled cancellation does not renew even with paid invoice`() {
        jdbc.update("""UPDATE tenant_subscriptions SET cancel_at_period_end=TRUE,
            current_period_start=CURRENT_TIMESTAMP-INTERVAL '40 days',
            current_period_end=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE tenant_id=?""", tenant)
        renewals.reconcileTenant(tenant)
        assertEquals("CANCELED", billing.platformSubscription(tenant).status)
    }
}
