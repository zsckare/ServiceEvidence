package com.serviceevidence.backend.evidence

import com.serviceevidence.backend.identity.MembershipRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import com.serviceevidence.backend.workorders.WorkOrderAssignmentRepository
import com.serviceevidence.backend.workorders.WorkOrderRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/** EN: Registers verified metadata only; upload transport requires object storage integration. ES: Registra metadatos, no recibe binarios. */
data class EvidenceInput(val kind: String = "PHOTO", @field:NotBlank val objectKey: String, @field:NotBlank val contentType: String, val byteSize: Long, @field:Pattern(regexp="[a-fA-F0-9]{64}") val sha256: String, val notes: String? = null)
data class EvidenceView(val id: UUID,val workOrderId: UUID,val kind: String,val objectKey: String,val contentType: String,val byteSize: Long,val sha256: String,val notes: String?,val createdAt: Instant)
private fun ServiceEvidence.view() = EvidenceView(id,workOrderId,kind,objectKey,contentType,byteSize,sha256,notes,createdAt)
@RestController @RequestMapping("/api/v1/work-orders/{workOrderId}/evidence")
class EvidenceController(private val access: TenantAccess,private val orders: WorkOrderRepository,private val assignments: WorkOrderAssignmentRepository,private val memberships: MembershipRepository,private val evidence: EvidenceRepository) {
 private fun actor(jwt: Jwt)=UUID.fromString(jwt.subject)
 private fun verify(tenant: UUID,workOrderId: UUID,jwt: Jwt) {
  if(orders.findByTenantIdAndId(tenant,workOrderId)==null) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  if(memberships.findByUserIdAndTenantId(actor(jwt),tenant)?.role=="TECHNICIAN" && !assignments.existsByTenantIdAndWorkOrderIdAndTechnicianId(tenant,workOrderId,actor(jwt))) throw ResponseStatusException(HttpStatus.NOT_FOUND)
 }
 @GetMapping fun list(@AuthenticationPrincipal jwt: Jwt,@PathVariable workOrderId: UUID): List<EvidenceView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN");verify(tenant,workOrderId,jwt)
  return evidence.findAllByTenantIdAndWorkOrderIdOrderByCreatedAtDesc(tenant,workOrderId).map { it.view() }
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional fun create(@AuthenticationPrincipal jwt: Jwt,@PathVariable workOrderId: UUID,@Valid @RequestBody input: EvidenceInput): EvidenceView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN");verify(tenant,workOrderId,jwt)
  if(input.kind !in setOf("PHOTO","VIDEO","DOCUMENT","SIGNATURE") || input.byteSize<=0 || input.byteSize>100_000_000 || input.objectKey.length>500 || input.contentType.length>120) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
  val o=orders.findByTenantIdAndId(tenant,workOrderId)!!
  if(o.status in setOf("CANCELLED","COMPLETED")) throw ResponseStatusException(HttpStatus.CONFLICT,"Order closed")
  return evidence.save(ServiceEvidence(tenantId=tenant,workOrderId=workOrderId,uploadedBy=actor(jwt),kind=input.kind,objectKey=input.objectKey,contentType=input.contentType,byteSize=input.byteSize,sha256=input.sha256.lowercase(),notes=input.notes)).view()
 }
}
