package com.serviceevidence.backend.notifications

import com.serviceevidence.backend.shared.tenant.TenantAccess
import jakarta.persistence.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/** EN: In-app notification inbox, not an email delivery guarantee. ES: Bandeja interna, no envío de correo. */
@Entity @Table(name="service_notifications")
class ServiceNotification(@Id var id:UUID=UUID.randomUUID(),@Column(name="tenant_id") var tenantId:UUID=UUID.randomUUID(),
 @Column(name="user_id") var userId:UUID=UUID.randomUUID(), var title:String="", @Column(columnDefinition="text") var message:String="",
 @Column(name="created_at") var createdAt:Instant=Instant.now(),@Column(name="read_at") var readAt:Instant?=null)
interface NotificationRepository:org.springframework.data.jpa.repository.JpaRepository<ServiceNotification,UUID> {
 fun findAllByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId:UUID,userId:UUID):List<ServiceNotification>
 fun findByTenantIdAndUserIdAndId(tenantId:UUID,userId:UUID,id:UUID):ServiceNotification?
}
data class NotificationView(val id:UUID,val title:String,val message:String,val createdAt:Instant,val readAt:Instant?)
@RestController @RequestMapping("/api/v1/notifications")
class NotificationController(private val access:TenantAccess,private val repo:NotificationRepository) {
 @GetMapping fun list(@AuthenticationPrincipal jwt:Jwt):List<NotificationView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  return repo.findAllByTenantIdAndUserIdOrderByCreatedAtDesc(tenant,UUID.fromString(jwt.subject)).map { NotificationView(it.id,it.title,it.message,it.createdAt,it.readAt) }
 }
 @PostMapping("/{id}/read") @Transactional fun read(@AuthenticationPrincipal jwt:Jwt,@PathVariable id:UUID):NotificationView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  val n=repo.findByTenantIdAndUserIdAndId(tenant,UUID.fromString(jwt.subject),id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  if(n.readAt==null)n.readAt=Instant.now()
  return NotificationView(n.id,n.title,n.message,n.createdAt,n.readAt)
 }
}
