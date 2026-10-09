package com.serviceevidence.backend.workorders

import com.serviceevidence.backend.customers.CustomerRepository
import com.serviceevidence.backend.identity.MembershipRepository
import com.serviceevidence.backend.locations.LocationRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

private val priorities = setOf("LOW", "MEDIUM", "HIGH", "URGENT")
private val statuses = setOf("DRAFT", "SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED")
private val transitions = mapOf("DRAFT" to setOf("SCHEDULED", "CANCELLED"), "SCHEDULED" to setOf("IN_PROGRESS", "CANCELLED"), "IN_PROGRESS" to setOf("COMPLETED", "CANCELLED"))

data class WorkOrderInput(@field:NotBlank @field:Size(max=180) val title: String, val customerId: UUID, val locationId: UUID, val description: String? = null, val priority: String = "MEDIUM", val scheduledAt: Instant? = null)
data class WorkOrderView(val id: UUID, val customerId: UUID, val locationId: UUID, val title: String, val description: String?, val priority: String, val status: String, val scheduledAt: Instant?, val startedAt: Instant?, val completedAt: Instant?, val createdAt: Instant, val updatedAt: Instant)
data class StatusInput(val status: String)
data class AssignmentInput(val technicianId: UUID, val primary: Boolean = false)
data class AssignmentView(val id: UUID, val technicianId: UUID, val primary: Boolean)
data class EventView(val action: String, val details: String?, val actorId: UUID, val createdAt: Instant)
fun WorkOrder.view() = WorkOrderView(id,customerId,locationId,title,description,priority,status,scheduledAt,startedAt,completedAt,createdAt,updatedAt)

/** EN: Work orders and transitions. ES: Órdenes y transiciones auditables. */
@RestController @RequestMapping("/api/v1/work-orders")
class WorkOrdersController(private val access: TenantAccess, private val orders: WorkOrderRepository, private val customers: CustomerRepository, private val locations: LocationRepository, private val memberships: MembershipRepository, private val assignments: WorkOrderAssignmentRepository, private val events: WorkOrderEventRepository) {
 private fun actor(jwt: Jwt) = UUID.fromString(jwt.subject)
 private fun order(tenant: UUID, id: UUID) = orders.findByTenantIdAndId(tenant,id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
 private fun log(o: WorkOrder, jwt: Jwt, action: String, details: String? = null) { events.save(WorkOrderEvent(tenantId=o.tenantId,workOrderId=o.id,actorId=actor(jwt),action=action,details=details)) }
 private fun verifyLocation(tenant: UUID, customerId: UUID, locationId: UUID) {
  if(customers.findByTenantIdAndId(tenant,customerId)?.active != true) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Customer is inactive or missing")
  val location=locations.findByTenantIdAndId(tenant,locationId)
  if(location?.customerId != customerId || !location.active) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Location is inactive or does not belong to customer")
 }
 private fun validate(input: WorkOrderInput) { if(input.priority !in priorities) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid priority") }
 private fun readable(tenant: UUID, jwt: Jwt, o: WorkOrder) {
  val membership=memberships.findByUserIdAndTenantId(actor(jwt),tenant)
  if(membership?.role=="TECHNICIAN" && !assignments.existsByTenantIdAndWorkOrderIdAndTechnicianId(tenant,o.id,actor(jwt))) throw ResponseStatusException(HttpStatus.NOT_FOUND)
 }
 @GetMapping fun list(@AuthenticationPrincipal jwt: Jwt,@RequestParam(required=false) status: String?,@RequestParam(defaultValue="0") page: Int,@RequestParam(defaultValue="20") size: Int): Page<WorkOrderView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  if(page<0 || size !in 1..100 || (status!=null && status !in statuses)) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
  val pageable=PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt"))
  // EN: Technicians must not list other technicians' orders. ES: No revelar órdenes de otros técnicos.
  if(memberships.findByUserIdAndTenantId(actor(jwt),tenant)?.role=="TECHNICIAN") throw ResponseStatusException(HttpStatus.FORBIDDEN,"Use /mine for technician orders")
  return (if(status==null) orders.findByTenantId(tenant,pageable) else orders.findByTenantIdAndStatus(tenant,status,pageable)).map { it.view() }
 }
 @GetMapping("/mine") fun mine(@AuthenticationPrincipal jwt: Jwt): List<WorkOrderView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  // EN: Assignment-scoped query; pagination to be added for large workloads.
  return assignments.findAllByTenantIdAndTechnicianId(tenant,actor(jwt)).asSequence().mapNotNull { orders.findByTenantIdAndId(tenant,it.workOrderId) }.map { it.view() }.toList()
 }
 @GetMapping("/{id}") fun get(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID): WorkOrderView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN");val o=order(tenant,id);readable(tenant,jwt,o);return o.view()
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional fun create(@AuthenticationPrincipal jwt: Jwt,@Valid @RequestBody input: WorkOrderInput): WorkOrderView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR");validate(input);verifyLocation(tenant,input.customerId,input.locationId)
  val o=orders.save(WorkOrder(tenantId=tenant,customerId=input.customerId,locationId=input.locationId,title=input.title.trim(),description=input.description,priority=input.priority,scheduledAt=input.scheduledAt,createdBy=actor(jwt)))
  log(o,jwt,"CREATED");return o.view()
 }
 @PutMapping("/{id}") @Transactional fun update(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID,@Valid @RequestBody input: WorkOrderInput): WorkOrderView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR");validate(input);verifyLocation(tenant,input.customerId,input.locationId)
  val o=order(tenant,id);if(o.status in setOf("COMPLETED","CANCELLED")) throw ResponseStatusException(HttpStatus.CONFLICT,"Order is closed")
  o.customerId=input.customerId;o.locationId=input.locationId;o.title=input.title.trim();o.description=input.description;o.priority=input.priority;o.scheduledAt=input.scheduledAt;o.updatedAt=Instant.now()
  log(o,jwt,"UPDATED");return orders.save(o).view()
 }
 @PostMapping("/{id}/status") @Transactional fun changeStatus(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID,@RequestBody input: StatusInput): WorkOrderView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN");val o=order(tenant,id);readable(tenant,jwt,o)
  if(input.status !in (transitions[o.status] ?: emptySet())) throw ResponseStatusException(HttpStatus.CONFLICT,"Invalid transition")
  if(memberships.findByUserIdAndTenantId(actor(jwt),tenant)?.role=="TECHNICIAN" && input.status !in setOf("IN_PROGRESS","COMPLETED")) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  val previous=o.status;o.status=input.status;o.updatedAt=Instant.now()
  if(o.status=="IN_PROGRESS") o.startedAt=Instant.now()
  if(o.status=="COMPLETED") o.completedAt=Instant.now()
  log(o,jwt,"STATUS_CHANGED","$previous -> ${o.status}");return orders.save(o).view()
 }
 @GetMapping("/{id}/assignments") fun assignments(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID): List<AssignmentView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN");val o=order(tenant,id);readable(tenant,jwt,o)
  return assignments.findAllByTenantIdAndWorkOrderId(tenant,id).map { AssignmentView(it.id,it.technicianId,it.primary) }
 }
 @PostMapping("/{id}/assignments") @ResponseStatus(HttpStatus.CREATED) @Transactional fun assign(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID,@RequestBody input: AssignmentInput): AssignmentView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR");val o=order(tenant,id)
  if(o.status in setOf("COMPLETED","CANCELLED")) throw ResponseStatusException(HttpStatus.CONFLICT)
  val member=memberships.findByUserIdAndTenantId(input.technicianId,tenant)
  if(member?.enabled!=true || member.role!="TECHNICIAN") throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Not an active technician in tenant")
  if(assignments.existsByTenantIdAndWorkOrderIdAndTechnicianId(tenant,id,input.technicianId)) throw ResponseStatusException(HttpStatus.CONFLICT,"Already assigned")
  if(input.primary && assignments.findAllByTenantIdAndWorkOrderId(tenant,id).any { it.primary }) throw ResponseStatusException(HttpStatus.CONFLICT,"Primary technician already assigned")
  val a=assignments.save(WorkOrderAssignment(tenantId=tenant,workOrderId=id,technicianId=input.technicianId,primary=input.primary));log(o,jwt,"ASSIGNED",input.technicianId.toString());return AssignmentView(a.id,a.technicianId,a.primary)
 }
 @DeleteMapping("/{id}/assignments/{technicianId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional fun unassign(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID,@PathVariable technicianId: UUID) {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR");val o=order(tenant,id)
  if(o.status in setOf("COMPLETED","CANCELLED")) throw ResponseStatusException(HttpStatus.CONFLICT)
  if(assignments.deleteByTenantIdAndWorkOrderIdAndTechnicianId(tenant,id,technicianId)==0L) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  log(o,jwt,"UNASSIGNED",technicianId.toString())
 }
 @GetMapping("/{id}/events") fun history(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID): List<EventView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN");val o=order(tenant,id);readable(tenant,jwt,o)
  return events.findAllByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(tenant,id).map { EventView(it.action,it.details,it.actorId,it.createdAt) }
 }
}
