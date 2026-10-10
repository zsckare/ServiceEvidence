package com.serviceevidence.backend.billing

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.OffsetDateTime
import java.util.UUID

/**
 * EN: Periodic billing reconciler. It never charges a card or fabricates a payment.
 * ES: Conciliador periódico. Nunca cobra una tarjeta ni inventa un pago.
 */
@Service
@EnableScheduling
class BillingRenewalService(
    private val jdbc: JdbcTemplate,
    transactionManager: PlatformTransactionManager,
    @Value("\${serviceevidence.billing.automation-enabled:true}") private val enabled: Boolean
) {
    private val tx = TransactionTemplate(transactionManager)

    /** EN: Each tenant is processed in its own transaction.
     * ES: Cada empresa se procesa en su propia transacción.
     */
    @Scheduled(fixedDelayString = "\${serviceevidence.billing.reconcile-delay-ms:3600000}")
    fun reconcileExpired() {
        if (!enabled) return
        val tenantIds = jdbc.query("""SELECT tenant_id FROM tenant_subscriptions
            WHERE current_period_end <= CURRENT_TIMESTAMP AND billing_status IN ('ACTIVE','TRIALING','PAST_DUE')
            ORDER BY current_period_end LIMIT 500""", { rs, _ -> rs.getObject(1, UUID::class.java) })
        tenantIds.forEach { tenantId ->
            try { tx.executeWithoutResult { reconcileTenant(tenantId) } }
            catch (ex: Exception) {
                // EN: Keep processing other tenants; the failed transaction rolls back.
                // ES: Continuar con otras empresas; la transacción fallida se revierte.
                org.slf4j.LoggerFactory.getLogger(javaClass).error("Billing reconciliation failed for tenant {}", tenantId, ex)
            }
        }
    }

    /** EN: Locking makes overlapping workers safe, and the run ledger prevents repeat renewal.
     * ES: El bloqueo evita carreras y el registro de ciclos impide renovaciones duplicadas.
     */
    fun reconcileTenant(tenantId: UUID) {
        val rows = jdbc.query("""SELECT billing_status, billing_period, current_period_start, current_period_end,
            cancel_at_period_end FROM tenant_subscriptions WHERE tenant_id=? FOR UPDATE""", { rs, _ -> Cycle(
            rs.getString(1), rs.getString(2), rs.getObject(3, OffsetDateTime::class.java),
            rs.getObject(4, OffsetDateTime::class.java), rs.getBoolean(5)
        ) }, tenantId)
        val cycle = rows.firstOrNull() ?: return
        if (cycle.end.isAfter(OffsetDateTime.now())) return
        if (cycle.cancel) {
            jdbc.update("UPDATE tenant_subscriptions SET billing_status='CANCELED', updated_at=CURRENT_TIMESTAMP WHERE tenant_id=?", tenantId)
            log(tenantId, "SUBSCRIPTION_CANCELED", "at_period_end")
            return
        }
        // EN: A paid invoice issued in the expiring period authorizes one renewal.
        // ES: Una factura pagada emitida en el período que vence autoriza una renovación.
        val paid = jdbc.queryForObject("""SELECT EXISTS(SELECT 1 FROM billing_invoices WHERE tenant_id=?
            AND status='PAID' AND issued_at >= ? AND issued_at < ?)""", Boolean::class.java,
            tenantId, cycle.start, cycle.end) == true
        if (!paid) {
            if (cycle.status != "PAST_DUE") {
                jdbc.update("UPDATE tenant_subscriptions SET billing_status='PAST_DUE', updated_at=CURRENT_TIMESTAMP WHERE tenant_id=?", tenantId)
                log(tenantId, "SUBSCRIPTION_PAST_DUE", cycle.end.toString())
            }
            return
        }
        // EN: A unique period-end record protects against repeated execution.
        // ES: Una fila única por fin de período protege contra ejecuciones repetidas.
        val inserted = jdbc.update("""INSERT INTO billing_renewal_runs(id,tenant_id,period_end,result)
            VALUES(?,?,?,'RENEWED') ON CONFLICT (tenant_id,period_end) DO NOTHING""",
            UUID.randomUUID(), tenantId, cycle.end)
        if (inserted == 0) return
        jdbc.update("""UPDATE tenant_subscriptions SET billing_status='ACTIVE',
            current_period_start=?, current_period_end=? +
                CASE WHEN billing_period='YEARLY' THEN INTERVAL '1 year' ELSE INTERVAL '1 month' END,
            updated_at=CURRENT_TIMESTAMP WHERE tenant_id=?""", cycle.end, cycle.end, tenantId)
        log(tenantId, "AUTOMATIC_RENEWAL", cycle.end.toString())
    }

    private fun log(tenantId: UUID, type: String, details: String) = jdbc.update(
        "INSERT INTO billing_events(id,tenant_id,event_type,details) VALUES(?,?,?,?)",
        UUID.randomUUID(), tenantId, type, details)

    private data class Cycle(val status: String, val period: String, val start: OffsetDateTime,
                             val end: OffsetDateTime, val cancel: Boolean)
}
