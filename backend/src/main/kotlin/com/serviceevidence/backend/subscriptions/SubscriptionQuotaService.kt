package com.serviceevidence.backend.subscriptions

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** EN: Transaction-scoped tenant row locks serialize quota checks.
 * ES: Bloqueos transaccionales por tenant serializan la comprobación de cuotas.
 * Call only inside @Transactional methods before inserting or reactivating records.
 */
@Service
class SubscriptionQuotaService(private val jdbc: JdbcTemplate) {
    fun requireUserSlot(tenantId: UUID) {
        lockTenant(tenantId)
        val limit = limit(tenantId, "max_users")
        val used = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tenant_memberships WHERE tenant_id = ? AND enabled = TRUE",
            Long::class.java, tenantId
        ) ?: 0L
        if (used >= limit) throw ResponseStatusException(HttpStatus.CONFLICT, "User quota reached")
    }

    fun requireWorkOrderSlot(tenantId: UUID) {
        lockTenant(tenantId)
        val limit = limit(tenantId, "max_work_orders_per_month")
        // EN: UTC calendar month. ES: Mes calendario UTC.
        val used = jdbc.queryForObject(
            """SELECT COUNT(*) FROM work_orders WHERE tenant_id = ?
                AND created_at >= (date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'UTC') AT TIME ZONE 'UTC')
                AND created_at < ((date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'UTC') + INTERVAL '1 month') AT TIME ZONE 'UTC')""",
            Long::class.java, tenantId
        ) ?: 0L
        if (used >= limit) throw ResponseStatusException(HttpStatus.CONFLICT, "Monthly work order quota reached")
    }

    private fun lockTenant(tenantId: UUID) {
        val rows = jdbc.query("SELECT id FROM tenants WHERE id = ? FOR UPDATE",
            { rs, _ -> rs.getObject("id", UUID::class.java) }, tenantId)
        if (rows.isEmpty()) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found")
    }

    private fun limit(tenantId: UUID, column: String): Long {
        // EN/ES: Column is an internal constant, never user-supplied input.
        return jdbc.query("""SELECT p.$column AS quota FROM tenant_subscriptions s
            JOIN subscription_plans p ON p.code = s.plan_code WHERE s.tenant_id = ?""",
            { rs, _ -> rs.getLong("quota") }, tenantId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Tenant has no subscription")
    }
}
