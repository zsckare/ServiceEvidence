package com.serviceevidence.backend.billing

import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

/** EN: Financial aggregates for platform administrators, in minor currency units.
 * ES: Agregados financieros para administradores globales, en unidades monetarias menores.
 */
data class FinancialCurrencySummary(
    val currency: String,
    val collectedMinor: Long,
    val invoicedMinor: Long,
    val outstandingMinor: Long,
    val overdueMinor: Long,
    val openInvoices: Long,
    val overdueInvoices: Long
)
data class FinancialSubscriptionSummary(
    val active: Long, val trialing: Long, val pastDue: Long,
    val canceled: Long, val expired: Long, val upcomingRenewals: Long
)
data class FinancialOverview(
    val from: LocalDate,
    val to: LocalDate,
    val currencies: List<FinancialCurrencySummary>,
    val subscriptions: FinancialSubscriptionSummary
)

@RestController
@RequestMapping("/api/v1/billing/platform/financial-overview")
class BillingMetricsController(private val jdbc: JdbcTemplate) {
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun overview(
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?
    ): FinancialOverview {
        val end = to ?: LocalDate.now()
        val start = from ?: end.minusDays(29)
        if (start.isAfter(end) || start.isBefore(end.minusYears(3)) || end.isAfter(LocalDate.now().plusDays(1))) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range (maximum 3 years)")
        }
        val endExclusive = end.plusDays(1)
        // EN: Aggregate payments by recorded date, independently from invoice issue date.
        // ES: Agrupar pagos por fecha de registro, independiente de la fecha de emisión.
        val currencies = jdbc.query("""
            WITH invoice_totals AS (
                SELECT currency,
                    COALESCE(SUM(amount_minor),0) AS invoiced,
                    COALESCE(SUM(CASE WHEN status IN ('OPEN','PARTIALLY_PAID')
                        THEN amount_minor-paid_amount_minor ELSE 0 END),0) AS outstanding,
                    COALESCE(SUM(CASE WHEN status IN ('OPEN','PARTIALLY_PAID') AND due_at < CURRENT_TIMESTAMP
                        THEN amount_minor-paid_amount_minor ELSE 0 END),0) AS overdue,
                    COUNT(*) FILTER (WHERE status IN ('OPEN','PARTIALLY_PAID')) AS open_count,
                    COUNT(*) FILTER (WHERE status IN ('OPEN','PARTIALLY_PAID') AND due_at < CURRENT_TIMESTAMP) AS overdue_count
                FROM billing_invoices WHERE issued_at >= ? AND issued_at < ? GROUP BY currency
            ), payment_totals AS (
                SELECT currency, COALESCE(SUM(amount_minor),0) AS collected
                FROM billing_payments WHERE recorded_at >= ? AND recorded_at < ? GROUP BY currency
            )
            SELECT COALESCE(i.currency,p.currency) AS currency,
                COALESCE(p.collected,0) AS collected,
                COALESCE(i.invoiced,0) AS invoiced,
                COALESCE(i.outstanding,0) AS outstanding,
                COALESCE(i.overdue,0) AS overdue,
                COALESCE(i.open_count,0) AS open_count,
                COALESCE(i.overdue_count,0) AS overdue_count
            FROM invoice_totals i FULL OUTER JOIN payment_totals p ON p.currency=i.currency
            ORDER BY currency
        """.trimIndent(), { rs, _ -> FinancialCurrencySummary(
            rs.getString("currency"), rs.getLong("collected"), rs.getLong("invoiced"),
            rs.getLong("outstanding"), rs.getLong("overdue"), rs.getLong("open_count"), rs.getLong("overdue_count")
        ) }, start.atStartOfDay().atOffset(java.time.ZoneOffset.UTC), endExclusive.atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
            start.atStartOfDay().atOffset(java.time.ZoneOffset.UTC), endExclusive.atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
        val subscriptions = jdbc.queryForObject("""
            SELECT COUNT(*) FILTER (WHERE billing_status='ACTIVE') AS active,
                COUNT(*) FILTER (WHERE billing_status='TRIALING') AS trialing,
                COUNT(*) FILTER (WHERE billing_status='PAST_DUE') AS past_due,
                COUNT(*) FILTER (WHERE billing_status='CANCELED') AS canceled,
                COUNT(*) FILTER (WHERE billing_status='EXPIRED') AS expired,
                COUNT(*) FILTER (WHERE billing_status IN ('ACTIVE','TRIALING')
                    AND current_period_end BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '7 days') AS upcoming
            FROM tenant_subscriptions
        """.trimIndent(), { rs, _ -> FinancialSubscriptionSummary(
            rs.getLong("active"), rs.getLong("trialing"), rs.getLong("past_due"),
            rs.getLong("canceled"), rs.getLong("expired"), rs.getLong("upcoming")
        ) })!!
        return FinancialOverview(start,end,currencies,subscriptions)
    }
}
