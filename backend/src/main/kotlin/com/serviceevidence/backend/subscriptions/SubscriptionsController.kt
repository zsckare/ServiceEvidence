package com.serviceevidence.backend.subscriptions

import com.serviceevidence.backend.shared.tenant.TenantAccess
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** EN: Plans are entitlements, not billing. ES: Los planes definen cuotas; no procesan pagos. */
data class PlanView(val code: String, val name: String, val maxUsers: Int, val maxWorkOrdersPerMonth: Int)
data class SubscriptionView(val tenantId: UUID, val plan: PlanView)
data class ChangePlanRequest(val planCode: String)

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionsController(private val jdbc: JdbcTemplate, private val access: TenantAccess) {
    @GetMapping("/plans")
    fun plans(): List<PlanView> = jdbc.query(
        "SELECT code, name, max_users, max_work_orders_per_month FROM subscription_plans WHERE active = TRUE ORDER BY max_users",
        { rs, _ -> PlanView(rs.getString("code"), rs.getString("name"), rs.getInt("max_users"), rs.getInt("max_work_orders_per_month")) }
    )

    @GetMapping("/mine")
    fun mine(@AuthenticationPrincipal jwt: Jwt): SubscriptionView {
        val tenantId = access.require(jwt, "TENANT_ADMIN")
        return subscription(tenantId)
    }

    @GetMapping("/platform/tenants/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun byTenant(@PathVariable tenantId: UUID): SubscriptionView = subscription(tenantId)

    @PutMapping("/platform/tenants/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    fun changePlan(@PathVariable tenantId: UUID, @RequestBody body: ChangePlanRequest): SubscriptionView {
        val code = body.planCode.trim().uppercase()
        if (plans().none { it.code == code }) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid plan")
        val updated = jdbc.update(
            """INSERT INTO tenant_subscriptions(tenant_id, plan_code) VALUES (?, ?)
                ON CONFLICT (tenant_id) DO UPDATE SET plan_code = EXCLUDED.plan_code, updated_at = CURRENT_TIMESTAMP""",
            tenantId, code
        )
        if (updated == 0) throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return subscription(tenantId)
    }

    private fun subscription(tenantId: UUID): SubscriptionView {
        return jdbc.query(
            """SELECT p.code, p.name, p.max_users, p.max_work_orders_per_month
               FROM tenant_subscriptions s JOIN subscription_plans p ON p.code = s.plan_code
               WHERE s.tenant_id = ?""",
            { rs, _ -> SubscriptionView(tenantId, PlanView(rs.getString("code"), rs.getString("name"),
                rs.getInt("max_users"), rs.getInt("max_work_orders_per_month"))) }, tenantId
        ).firstOrNull() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found")
    }
}
