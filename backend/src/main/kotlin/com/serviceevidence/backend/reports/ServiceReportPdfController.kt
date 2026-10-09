package com.serviceevidence.backend.reports

import com.serviceevidence.backend.customers.CustomerRepository
import com.serviceevidence.backend.evidence.EvidenceRepository
import com.serviceevidence.backend.identity.MembershipRepository
import com.serviceevidence.backend.locations.LocationRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import com.serviceevidence.backend.workorders.WorkOrderAssignmentRepository
import com.serviceevidence.backend.workorders.WorkOrderRepository
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** EN: Generates a tenant-authorized service report PDF. ES: Genera un reporte PDF autorizado por empresa. */
@RestController @RequestMapping("/api/v1/work-orders/{workOrderId}/report.pdf")
class ServiceReportPdfController(private val access:TenantAccess,private val orders:WorkOrderRepository,
 private val customers:CustomerRepository,private val locations:LocationRepository,
 private val evidence:EvidenceRepository,private val memberships:MembershipRepository,
 private val assignments:WorkOrderAssignmentRepository,
 private val jasper:ServiceReportJasperService) {
 @GetMapping(produces=[MediaType.APPLICATION_PDF_VALUE])
 fun report(@AuthenticationPrincipal jwt:Jwt,@PathVariable workOrderId:UUID):ResponseEntity<ByteArray> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  val order=orders.findByTenantIdAndId(tenant,workOrderId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  val actor=UUID.fromString(jwt.subject)
  if(memberships.findByUserIdAndTenantId(actor,tenant)?.role=="TECHNICIAN" && !assignments.existsByTenantIdAndWorkOrderIdAndTechnicianId(tenant,workOrderId,actor)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  val customer=customers.findByTenantIdAndId(tenant,order.customerId)
  val location=locations.findByTenantIdAndId(tenant,order.locationId)
  val items=evidence.findAllByTenantIdAndWorkOrderIdOrderByCreatedAtDesc(tenant,workOrderId)
  // EN: Keep tenant checks in the controller; delegate presentation to JasperReports.
  // ES: Conserva la autorización multiempresa y delega la presentación a JasperReports.
  val output = jasper.generate(order, customer?.name ?: "Unavailable", location?.name ?: "Unavailable", items)
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=service-report-$workOrderId.pdf")
   .body(output)
 }
}
