package com.serviceevidence.backend.subscriptions

import com.serviceevidence.backend.shared.tenant.TenantAccess
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.UUID

/** EN: Current subscription usage; all queries are scoped by tenant ID.
 *  ES: Consumo de la suscripción; todas las consultas se limitan al tenant.
 */
data class QuotaUsage(val used: Long, val limit: Int)
data class SubscriptionUsageView(
    val tenantId: UUID,
    val planCode: String,
    val planName: String,
    val users: QuotaUsage,
    val monthlyWorkOrders: QuotaUsage,
    val monthUtc: String
)

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionUsageController(
    private val jdbc: JdbcTemplate,
    private val access: TenantAccess
) {
    @GetMapping("/usage/mine")
    fun mine(@AuthenticationPrincipal jwt: Jwt): SubscriptionUsageView =
        usage(access.require(jwt, "TENANT_ADMIN"))

    @GetMapping("/platform/tenants/{tenantId}/usage")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun byTenant(@PathVariable tenantId: UUID): SubscriptionUsageView = usage(tenantId)

    private fun usage(tenantId: UUID): SubscriptionUsageView {
        val plan = jdbc.query(
            """SELECT p.code, p.name, p.max_users, p.max_work_orders_per_month
               FROM tenant_subscriptions s
               JOIN subscription_plans p ON p.code = s.plan_code
               WHERE s.tenant_id = ?""",
            { rs, _ -> PlanView(rs.getString("code"), rs.getString("name"),
                rs.getInt("max_users"), rs.getInt("max_work_orders_per_month")) }, tenantId
        ).firstOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found")

        val activeUsers = jdbc.queryForObject(
            "SELECT COUNT(*) FROM tenant_memberships WHERE tenant_id = ? AND enabled = TRUE",
            Long::class.java, tenantId
        ) ?: 0L
        val monthlyOrders = jdbc.queryForObject(
            """SELECT COUNT(*) FROM work_orders WHERE tenant_id = ?
               AND created_at >= (date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'UTC') AT TIME ZONE 'UTC')
               AND created_at < ((date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'UTC') + INTERVAL '1 month') AT TIME ZONE 'UTC')""",
            Long::class.java, tenantId
        ) ?: 0L
        val monthUtc = jdbc.queryForObject(
            "SELECT to_char(CURRENT_TIMESTAMP AT TIME ZONE 'UTC', 'YYYY-MM')", String::class.java
        ) ?: ""
        return SubscriptionUsageView(tenantId, plan.code, plan.name,
            QuotaUsage(activeUsers, plan.maxUsers),
            QuotaUsage(monthlyOrders, plan.maxWorkOrdersPerMonth), monthUtc)
    }
}
