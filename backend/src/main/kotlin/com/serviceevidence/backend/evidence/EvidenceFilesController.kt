package com.serviceevidence.backend.evidence

import com.serviceevidence.backend.identity.MembershipRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import com.serviceevidence.backend.workorders.WorkOrderAssignmentRepository
import com.serviceevidence.backend.workorders.WorkOrderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID

/** EN: Private tenant-scoped binary evidence. ES: Evidencia binaria privada por tenant. */
@RestController
@RequestMapping("/api/v1/work-orders/{workOrderId}/files")
class EvidenceFilesController(
 private val access: TenantAccess, private val orders: WorkOrderRepository,
 private val assignments: WorkOrderAssignmentRepository, private val memberships: MembershipRepository,
 private val evidence: EvidenceRepository,
 @Value("\${serviceevidence.storage.path:./private-evidence}") private val storagePath: String
) {
 private val allowed = mapOf("image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp", "application/pdf" to "pdf")
 private fun verify(jwt: Jwt, orderId: UUID): UUID {
  val tenant = access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  if (orders.findByTenantIdAndId(tenant,orderId)==null) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  val user = UUID.fromString(jwt.subject)
  if (memberships.findByUserIdAndTenantId(user,tenant)?.role=="TECHNICIAN" && !assignments.existsByTenantIdAndWorkOrderIdAndTechnicianId(tenant,orderId,user)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  return tenant
 }
 @PostMapping(consumes=[MediaType.MULTIPART_FORM_DATA_VALUE])
 @ResponseStatus(HttpStatus.CREATED)
 @Transactional
 fun upload(@AuthenticationPrincipal jwt: Jwt, @PathVariable workOrderId: UUID,
  @RequestParam("file") file: MultipartFile, @RequestParam(defaultValue="PHOTO") kind: String,
  @RequestParam(required=false) notes: String?): EvidenceView {
  val tenant=verify(jwt,workOrderId)
  if (orders.findByTenantIdAndId(tenant,workOrderId)!!.status in setOf("CANCELLED","COMPLETED")) throw ResponseStatusException(HttpStatus.CONFLICT)
  val type=file.contentType?.lowercase() ?: ""
  val ext=allowed[type] ?: throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
  if (file.isEmpty || file.size>20_000_000L || kind !in setOf("PHOTO","DOCUMENT","SIGNATURE")) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
  if (kind=="SIGNATURE" && type !in setOf("image/png","image/jpeg")) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
  val bytes=file.bytes
  val valid=when(type) {
   "image/png" -> bytes.size>=8 && bytes.take(8).map { it.toInt() and 255 }==listOf(137,80,78,71,13,10,26,10)
   "image/jpeg" -> bytes.size>=3 && (bytes[0].toInt() and 255)==255 && (bytes[1].toInt() and 255)==216 && (bytes[2].toInt() and 255)==255
   "image/webp" -> bytes.size>=12 && String(bytes,0,4)=="RIFF" && String(bytes,8,4)=="WEBP"
   else -> bytes.size>=5 && String(bytes,0,5)=="%PDF-"
  }
  if(!valid) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid file signature")
  val id=UUID.randomUUID()
  val root=Path.of(storagePath).toAbsolutePath().normalize()
  val dir=root.resolve(tenant.toString()).resolve(workOrderId.toString())
  Files.createDirectories(dir)
  val path=dir.resolve("$id.$ext")
  Files.write(path,bytes)
  val sha=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  return try {
   val entity=evidence.save(ServiceEvidence(id=id,tenantId=tenant,workOrderId=workOrderId,uploadedBy=UUID.fromString(jwt.subject),kind=kind,objectKey="$id.$ext",contentType=type,byteSize=bytes.size.toLong(),sha256=sha,notes=notes))
   EvidenceView(entity.id,entity.workOrderId,entity.kind,entity.objectKey,entity.contentType,entity.byteSize,entity.sha256,entity.notes,entity.createdAt)
  } catch(ex: Exception) { Files.deleteIfExists(path);throw ex }
 }
 @GetMapping("/{evidenceId}")
 fun download(@AuthenticationPrincipal jwt: Jwt,@PathVariable workOrderId: UUID,@PathVariable evidenceId: UUID): ResponseEntity<FileSystemResource> {
  val tenant=verify(jwt,workOrderId)
  val item=evidence.findByTenantIdAndId(tenant,evidenceId)?.takeIf { it.workOrderId==workOrderId } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  // EN: Legacy metadata keys are not trusted as filesystem paths. ES: No confiar en rutas suministradas por clientes.
  if (!item.objectKey.matches(Regex("[0-9a-f-]{36}\\.(jpg|jpeg|png|webp|pdf)"))) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  val root=Path.of(storagePath).toAbsolutePath().normalize()
  val path=root.resolve(tenant.toString()).resolve(workOrderId.toString()).resolve(item.objectKey).normalize()
  if (!path.startsWith(root) || !Files.isRegularFile(path)) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(item.contentType))
   .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"evidence-${item.id}.${item.objectKey.substringAfterLast('.')}\"")
   .header("X-Content-Type-Options","nosniff")
   .body(FileSystemResource(path))
 }
}
