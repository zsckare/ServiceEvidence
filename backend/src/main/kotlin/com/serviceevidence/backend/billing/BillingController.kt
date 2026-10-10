package com.serviceevidence.backend.billing

import com.serviceevidence.backend.shared.tenant.TenantAccess
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

/** EN: Internal billing ledger; no external payment processing.
 * ES: Libro de facturación interno; no procesa cobros externos.
 */
data class BillingSubscription(val tenantId: UUID, val planCode: String, val status: String,
    val period: String, val periodStart: OffsetDateTime, val periodEnd: OffsetDateTime,
    val trialEndsAt: OffsetDateTime?, val cancelAtPeriodEnd: Boolean)
data class BillingInvoice(val id: UUID, val number: String, val planCode: String, val status: String,
    val currency: String, val amountMinor: Long, val issuedAt: OffsetDateTime,
    val dueAt: OffsetDateTime, val paidAt: OffsetDateTime?)
data class BillingPayment(val id: UUID, val invoiceId: UUID, val amountMinor: Long,
    val currency: String, val method: String, val reference: String?, val recordedAt: OffsetDateTime)
data class StartTrialRequest(val days: Int = 14)
data class IssueInvoiceRequest(val dueDays: Int = 15)
data class RecordPaymentRequest(val method: String, val reference: String? = null)
data class CancelSubscriptionRequest(val atPeriodEnd: Boolean = true)

@RestController
@RequestMapping("/api/v1/billing")
class BillingController(private val jdbc: JdbcTemplate, private val access: TenantAccess) {
    @GetMapping("/mine")
    fun mine(@AuthenticationPrincipal jwt: Jwt): BillingSubscription = subscription(access.require(jwt, "TENANT_ADMIN"))

    @GetMapping("/invoices/mine")
    fun myInvoices(@AuthenticationPrincipal jwt: Jwt): List<BillingInvoice> = invoices(access.require(jwt, "TENANT_ADMIN"))

    @GetMapping("/platform/tenants/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun platformSubscription(@PathVariable tenantId: UUID): BillingSubscription = subscription(tenantId)

    @GetMapping("/platform/tenants/{tenantId}/invoices")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun platformInvoices(@PathVariable tenantId: UUID): List<BillingInvoice> = invoices(tenantId)

    @GetMapping("/platform/tenants/{tenantId}/payments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun payments(@PathVariable tenantId: UUID): List<BillingPayment> = jdbc.query(
        """SELECT id, invoice_id, amount_minor, currency, method, external_reference, recorded_at
           FROM billing_payments WHERE tenant_id = ? ORDER BY recorded_at DESC""",
        { rs, _ -> BillingPayment(rs.getObject("id", UUID::class.java), rs.getObject("invoice_id", UUID::class.java),
            rs.getLong("amount_minor"), rs.getString("currency"), rs.getString("method"),
            rs.getString("external_reference"), rs.getObject("recorded_at", OffsetDateTime::class.java)) }, tenantId)

    @PostMapping("/platform/tenants/{tenantId}/trial")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    fun startTrial(@PathVariable tenantId: UUID, @RequestBody body: StartTrialRequest): BillingSubscription {
        if (body.days !in 1..90) badRequest("Trial must be 1–90 days")
        lockSubscription(tenantId)
        val existing = subscription(tenantId)
        if (existing.trialEndsAt != null) conflict("Trial already used")
        jdbc.update("""UPDATE tenant_subscriptions SET billing_status='TRIALING', trial_ends_at=CURRENT_TIMESTAMP + (? * INTERVAL '1 day'),
            current_period_start=CURRENT_TIMESTAMP, current_period_end=CURRENT_TIMESTAMP + (? * INTERVAL '1 day'),
            cancel_at_period_end=FALSE, updated_at=CURRENT_TIMESTAMP WHERE tenant_id=?""", body.days, body.days, tenantId)
        event(tenantId, "TRIAL_STARTED", "days=${body.days}")
        return subscription(tenantId)
    }

    @PostMapping("/platform/tenants/{tenantId}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    fun cancel(@PathVariable tenantId: UUID, @RequestBody body: CancelSubscriptionRequest): BillingSubscription {
        lockSubscription(tenantId)
        jdbc.update("""UPDATE tenant_subscriptions SET cancel_at_period_end=?, billing_status=CASE WHEN ? THEN billing_status ELSE 'CANCELED' END,
            updated_at=CURRENT_TIMESTAMP WHERE tenant_id=?""", body.atPeriodEnd, body.atPeriodEnd, tenantId)
        event(tenantId, "CANCELLATION_REQUESTED", "atPeriodEnd=${body.atPeriodEnd}")
        return subscription(tenantId)
    }

    @PostMapping("/platform/tenants/{tenantId}/renew")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    fun renew(@PathVariable tenantId: UUID): BillingSubscription {
        lockSubscription(tenantId)
        val current = subscription(tenantId)
        if (current.cancelAtPeriodEnd || current.status == "CANCELED") conflict("Subscription canceled")
        jdbc.update("""UPDATE tenant_subscriptions SET billing_status='ACTIVE',
            current_period_start=GREATEST(current_period_end, CURRENT_TIMESTAMP),
            current_period_end=GREATEST(current_period_end, CURRENT_TIMESTAMP) +
                CASE WHEN billing_period='YEARLY' THEN INTERVAL '1 year' ELSE INTERVAL '1 month' END,
            updated_at=CURRENT_TIMESTAMP WHERE tenant_id=?""", tenantId)
        event(tenantId, "MANUAL_RENEWAL", null)
        return subscription(tenantId)
    }

    @PostMapping("/platform/tenants/{tenantId}/invoices")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    fun issueInvoice(@PathVariable tenantId: UUID, @RequestBody body: IssueInvoiceRequest): BillingInvoice {
        if (body.dueDays !in 1..90) badRequest("Due date must be 1–90 days")
        lockSubscription(tenantId)
        val subscription = subscription(tenantId)
        if (subscription.status == "CANCELED") conflict("Subscription canceled")
        val plan = jdbc.query("SELECT monthly_price_minor, currency FROM subscription_plans WHERE code=?",
            { rs, _ -> rs.getLong("monthly_price_minor") to rs.getString("currency") }, subscription.planCode).first()
        val amount = if (subscription.period == "YEARLY") Math.multiplyExact(plan.first, 12L) else plan.first
        if (amount <= 0L) conflict("Plan price is not configured")
        val id = UUID.randomUUID()
        val number = "SE-${UUID.randomUUID().toString().replace("-", "").take(16).uppercase()}"
        jdbc.update("""INSERT INTO billing_invoices(id,tenant_id,invoice_number,plan_code,currency,amount_minor,due_at)
            VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP + (? * INTERVAL '1 day'))""",
            id, tenantId, number, subscription.planCode, plan.second, amount, body.dueDays)
        event(tenantId, "INVOICE_ISSUED", id.toString())
        return invoices(tenantId).first { it.id == id }
    }

    @PostMapping("/platform/tenants/{tenantId}/invoices/{invoiceId}/payments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    fun recordPayment(@PathVariable tenantId: UUID, @PathVariable invoiceId: UUID,
                      @RequestBody body: RecordPaymentRequest, @AuthenticationPrincipal jwt: Jwt): BillingPayment {
        val method = body.method.uppercase()
        if (method !in setOf("BANK_TRANSFER", "CASH", "OTHER")) badRequest("Invalid payment method")
        if (body.reference != null && body.reference.length > 120) badRequest("Reference too long")
        val invoice = jdbc.query("""SELECT amount_minor, currency, status FROM billing_invoices
            WHERE id=? AND tenant_id=? FOR UPDATE""", { rs, _ -> Triple(rs.getLong(1), rs.getString(2), rs.getString(3)) }, invoiceId, tenantId)
            .firstOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found")
        if (invoice.third != "OPEN") conflict("Invoice already settled or void")
        val paymentId = UUID.randomUUID()
        val actorId = runCatching { UUID.fromString(jwt.subject) }.getOrNull()
        jdbc.update("""INSERT INTO billing_payments(id,invoice_id,tenant_id,amount_minor,currency,method,external_reference,recorded_by)
            VALUES(?,?,?,?,?,?,?,?)""", paymentId, invoiceId, tenantId, invoice.first, invoice.second, method,
            body.reference?.trim(), actorId)
        jdbc.update("UPDATE billing_invoices SET status='PAID', paid_at=CURRENT_TIMESTAMP WHERE id=?", invoiceId)
        event(tenantId, "PAYMENT_RECORDED", invoiceId.toString())
        return payments(tenantId).first { it.id == paymentId }
    }

    private fun lockSubscription(tenantId: UUID) {
        val found = jdbc.query("SELECT tenant_id FROM tenant_subscriptions WHERE tenant_id=? FOR UPDATE",
            { rs, _ -> rs.getObject(1, UUID::class.java) }, tenantId)
        if (found.isEmpty()) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found")
    }

    private fun subscription(tenantId: UUID): BillingSubscription = jdbc.query("""SELECT tenant_id, plan_code, billing_status,
        billing_period, current_period_start, current_period_end, trial_ends_at, cancel_at_period_end
        FROM tenant_subscriptions WHERE tenant_id=?""", { rs, _ -> BillingSubscription(
        rs.getObject("tenant_id", UUID::class.java), rs.getString("plan_code"), rs.getString("billing_status"),
        rs.getString("billing_period"), rs.getObject("current_period_start", OffsetDateTime::class.java),
        rs.getObject("current_period_end", OffsetDateTime::class.java),
        rs.getObject("trial_ends_at", OffsetDateTime::class.java), rs.getBoolean("cancel_at_period_end")) }, tenantId)
        .firstOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found")

    private fun invoices(tenantId: UUID): List<BillingInvoice> = jdbc.query("""SELECT id, invoice_number, plan_code, status,
        currency, amount_minor, issued_at, due_at, paid_at FROM billing_invoices WHERE tenant_id=? ORDER BY issued_at DESC""",
        { rs, _ -> BillingInvoice(rs.getObject("id", UUID::class.java), rs.getString("invoice_number"), rs.getString("plan_code"),
            rs.getString("status"), rs.getString("currency"), rs.getLong("amount_minor"),
            rs.getObject("issued_at", OffsetDateTime::class.java), rs.getObject("due_at", OffsetDateTime::class.java),
            rs.getObject("paid_at", OffsetDateTime::class.java)) }, tenantId)

    private fun event(tenantId: UUID, type: String, details: String?) {
        jdbc.update("INSERT INTO billing_events(id,tenant_id,event_type,details) VALUES(?,?,?,?)",
            UUID.randomUUID(), tenantId, type, details)
    }
    private fun badRequest(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
    private fun conflict(message: String): Nothing = throw ResponseStatusException(HttpStatus.CONFLICT, message)
}
