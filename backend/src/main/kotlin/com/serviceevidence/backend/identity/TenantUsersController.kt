package com.serviceevidence.backend.identity

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import com.serviceevidence.backend.tenants.TenantRepository
import java.util.UUID

data class CreateUserRequest(
 @field:Email val email: String,
 @field:NotBlank val fullName: String,
 @field:Size(min=12) val password: String,
 val role: String
)
data class UpdateMemberRequest(val role: String? = null, val enabled: Boolean? = null)

data class MemberView(val id: UUID, val email: String, val fullName: String, val role: String, val enabled: Boolean)

/** EN: All queries are scoped to the tenant in a signed JWT. ES: Consultas limitadas al tenant del JWT firmado. */
@RestController @RequestMapping("/api/v1/users")
class TenantUsersController(
 private val users: UserRepository, private val memberships: MembershipRepository,
 private val tenants: TenantRepository, private val passwords: PasswordEncoder,
 private val audits: AuditRepository
) {
 private fun authorizedTenant(jwt: Jwt): UUID {
  val tenantId = jwt.getClaimAsString("tenant_id")?.let(UUID::fromString)
   ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Select a tenant first")
  val tenant = tenants.findById(tenantId).orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN) }
  if (!tenant.active) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  val actor = users.findById(UUID.fromString(jwt.subject)).orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
  if (!actor.enabled) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
  val membership = memberships.findByUserIdAndTenantId(actor.id,tenantId)
  if (!actor.platformAdmin && (membership?.enabled != true || membership.role != "TENANT_ADMIN"))
   throw ResponseStatusException(HttpStatus.FORBIDDEN)
  return tenantId
 }
 @GetMapping
 fun list(@AuthenticationPrincipal jwt: Jwt): List<MemberView> {
  val tenantId = authorizedTenant(jwt)
  return memberships.findAllByTenantId(tenantId).mapNotNull { m ->
   users.findById(m.userId).orElse(null)?.let { MemberView(it.id,it.email,it.fullName,m.role,m.enabled) }
  }
 }
 /** EN: Update tenant membership only, never the global user account.
  * ES: Modifica únicamente la membresía del tenant, no la cuenta global.
  */
 @PatchMapping("/{userId}") @Transactional
 fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable userId: UUID,
            @RequestBody req: UpdateMemberRequest): MemberView {
  val tenantId = authorizedTenant(jwt)
  val membership = memberships.findByUserIdAndTenantId(userId, tenantId)
   ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found")
  val member = users.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
  if (req.role == null && req.enabled == null)
   throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No changes requested")
  if (req.role != null && req.role !in setOf("TENANT_ADMIN", "SUPERVISOR", "TECHNICIAN", "CLIENT"))
   throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role")
  // EN: Prevent an administrator from removing their own tenant admin access.
  // ES: Evita que un administrador se quite a sí mismo el acceso administrativo.
  if (userId.toString() == jwt.subject &&
      (req.enabled == false || (req.role != null && req.role != "TENANT_ADMIN")))
   throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot revoke your own administrator access")
  req.role?.let { membership.role = it }
  req.enabled?.let { membership.enabled = it }
  memberships.save(membership)
  audits.save(SecurityAudit(actorUserId=UUID.fromString(jwt.subject), tenantId=tenantId,
   action="MEMBERSHIP_UPDATED", targetId=userId.toString()))
  return MemberView(member.id, member.email, member.fullName, membership.role, membership.enabled)
 }

 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
 fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody req: CreateUserRequest): MemberView {
  val tenantId = authorizedTenant(jwt)
  if (req.role !in setOf("TENANT_ADMIN","SUPERVISOR","TECHNICIAN","CLIENT"))
   throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role")
  val email = req.email.trim().lowercase()
  if (users.findByEmail(email) != null) throw ResponseStatusException(HttpStatus.CONFLICT,"Email already registered")
  val user = users.save(PlatformUser(email=email,fullName=req.fullName.trim(),passwordHash=requireNotNull(passwords.encode(req.password)) { "Password encoder returned null" }))
  if (memberships.findByUserIdAndTenantId(user.id,tenantId) != null)
   throw ResponseStatusException(HttpStatus.CONFLICT,"Membership already exists")
  // EN: Existing users are never given a new password by another tenant.
  // ES: Otro tenant nunca puede cambiar la contraseña de un usuario existente.
  memberships.save(TenantMembership(tenantId=tenantId,userId=user.id,role=req.role))
  audits.save(SecurityAudit(actorUserId=UUID.fromString(jwt.subject),tenantId=tenantId,action="USER_ASSIGNED",targetId=user.id.toString()))
  return MemberView(user.id,user.email,user.fullName,req.role,true)
 }
}
