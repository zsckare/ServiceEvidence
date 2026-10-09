package com.serviceevidence.backend.reports

import com.serviceevidence.backend.shared.tenant.TenantAccess
import com.serviceevidence.backend.workorders.WorkOrderRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** EN: Tenant-scoped operational KPIs. ES: Indicadores operativos aislados por empresa. */
data class WorkOrderSummary(val total: Long,val draft: Long,val scheduled: Long,val inProgress: Long,val completed: Long,val cancelled: Long)
@RestController @RequestMapping("/api/v1/reports")
class ReportsController(private val access: TenantAccess,private val orders: WorkOrderRepository) {
 @GetMapping("/work-orders/summary") fun summary(@AuthenticationPrincipal jwt: Jwt): WorkOrderSummary {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR")
  val draft=orders.countByTenantIdAndStatus(tenant,"DRAFT")
  val scheduled=orders.countByTenantIdAndStatus(tenant,"SCHEDULED")
  val inProgress=orders.countByTenantIdAndStatus(tenant,"IN_PROGRESS")
  val completed=orders.countByTenantIdAndStatus(tenant,"COMPLETED")
  val cancelled=orders.countByTenantIdAndStatus(tenant,"CANCELLED")
  return WorkOrderSummary(draft+scheduled+inProgress+completed+cancelled,draft,scheduled,inProgress,completed,cancelled)
 }
}
