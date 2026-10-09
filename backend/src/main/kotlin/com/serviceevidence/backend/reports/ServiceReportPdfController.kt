package com.serviceevidence.backend.reports

import com.serviceevidence.backend.customers.CustomerRepository
import com.serviceevidence.backend.evidence.EvidenceRepository
import com.serviceevidence.backend.identity.MembershipRepository
import com.serviceevidence.backend.locations.LocationRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import com.serviceevidence.backend.workorders.WorkOrderAssignmentRepository
import com.serviceevidence.backend.workorders.WorkOrderRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayOutputStream
import java.util.UUID

/** EN: Generates a tenant-authorized service report PDF. ES: Genera un reporte PDF autorizado por empresa. */
@RestController @RequestMapping("/api/v1/work-orders/{workOrderId}/report.pdf")
class ServiceReportPdfController(private val access:TenantAccess,private val orders:WorkOrderRepository,
 private val customers:CustomerRepository,private val locations:LocationRepository,
 private val evidence:EvidenceRepository,private val memberships:MembershipRepository,
 private val assignments:WorkOrderAssignmentRepository) {
 @GetMapping(produces=[MediaType.APPLICATION_PDF_VALUE])
 fun report(@AuthenticationPrincipal jwt:Jwt,@PathVariable workOrderId:UUID):ResponseEntity<ByteArray> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  val order=orders.findByTenantIdAndId(tenant,workOrderId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  val actor=UUID.fromString(jwt.subject)
  if(memberships.findByUserIdAndTenantId(actor,tenant)?.role=="TECHNICIAN" && !assignments.existsByTenantIdAndWorkOrderIdAndTechnicianId(tenant,workOrderId,actor)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  val customer=customers.findByTenantIdAndId(tenant,order.customerId)
  val location=locations.findByTenantIdAndId(tenant,order.locationId)
  val items=evidence.findAllByTenantIdAndWorkOrderIdOrderByCreatedAtDesc(tenant,workOrderId)
  val lines=listOf("SERVICE REPORT", "Order: ${order.id}", "Title: ${order.title}", "Status: ${order.status}", "Priority: ${order.priority}", "Customer: ${customer?.name ?: "Unavailable"}", "Location: ${location?.name ?: "Unavailable"}", "Created: ${order.createdAt}", "Scheduled: ${order.scheduledAt ?: "Not scheduled"}", "Completed: ${order.completedAt ?: "Not completed"}", "Evidence items: ${items.size}") + items.take(15).map { "${it.kind}: ${it.id}" }
  val output=ByteArrayOutputStream()
  PDDocument().use { doc ->
   val page=PDPage(PDRectangle.LETTER);doc.addPage(page)
   PDPageContentStream(doc,page).use { stream ->
    stream.beginText();stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA),10f)
    stream.newLineAtOffset(48f,740f)
    lines.forEach { raw ->
     // EN: Helvetica is limited to WinAnsi; replace unsupported glyphs. ES: Sustituye caracteres no compatibles.
     val safe=raw.map { if(it.code in 32..126) it else '?' }.joinToString("").take(110)
     stream.showText(safe);stream.newLineAtOffset(0f,-19f)
    }
    stream.endText()
   }
   doc.save(output)
  }
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=service-report-$workOrderId.pdf")
   .body(output.toByteArray())
 }
}
