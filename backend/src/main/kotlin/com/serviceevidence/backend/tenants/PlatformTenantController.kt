package com.serviceevidence.backend.tenants

import com.serviceevidence.backend.identity.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import org.springframework.jdbc.core.JdbcTemplate

data class CreateTenantRequest(
 @field:NotBlank val name: String,
 @field:NotBlank @field:Size(min=2,max=50) val code: String,
 @field:Email val adminEmail: String,
 @field:NotBlank val adminName: String,
 @field:Size(min=12) val adminPassword: String
)
data class TenantView(val id: UUID, val code: String, val name: String, val active: Boolean)
data class StatusRequest(val active: Boolean)

@RestController @RequestMapping("/api/v1/platform/tenants") @PreAuthorize("hasRole('SUPER_ADMIN')")
class PlatformTenantController(
 private val tenants: TenantRepository, private val users: UserRepository,
 private val memberships: MembershipRepository, private val passwords: PasswordEncoder,
 private val audits: AuditRepository, private val jdbc: JdbcTemplate
) {
 @GetMapping fun list(): List<TenantView> = tenants.findAll().map { TenantView(it.id,it.code,it.name,it.active) }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
 fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody req: CreateTenantRequest): TenantView {
  val code = req.code.trim().uppercase()
  if (!code.matches(Regex("[A-Z0-9_-]{2,50}"))) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid code")
  if (tenants.existsByCode(code)) throw ResponseStatusException(HttpStatus.CONFLICT,"Tenant code exists")
  val email = req.adminEmail.trim().lowercase()
  if (users.findByEmail(email) != null) throw ResponseStatusException(HttpStatus.CONFLICT,"Admin email already exists")
  val tenant = tenants.save(Tenant(code=code,name=req.name.trim()))
  // EN: New tenants start on Starter. ES: Los nuevos tenants inician en Starter.
  jdbc.update("INSERT INTO tenant_subscriptions (tenant_id, plan_code) VALUES (?, ?)", tenant.id, "STARTER")
  val admin = users.save(PlatformUser(email=email,fullName=req.adminName.trim(),passwordHash=requireNotNull(passwords.encode(req.adminPassword)) { "Password encoder returned null" }))
  memberships.save(TenantMembership(tenantId=tenant.id,userId=admin.id,role="TENANT_ADMIN"))
  audits.save(SecurityAudit(actorUserId=UUID.fromString(jwt.subject),tenantId=tenant.id,action="TENANT_CREATED",targetId=tenant.id.toString()))
  return TenantView(tenant.id,tenant.code,tenant.name,tenant.active)
 }
 @PatchMapping("/{id}/status") @Transactional
 fun status(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID,@RequestBody req: StatusRequest): TenantView {
  val tenant = tenants.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
  tenant.active=req.active
  audits.save(SecurityAudit(actorUserId=UUID.fromString(jwt.subject),tenantId=id,action="TENANT_STATUS_CHANGED",targetId=id.toString()))
  return TenantView(tenant.id,tenant.code,tenant.name,tenant.active)
 }
}
