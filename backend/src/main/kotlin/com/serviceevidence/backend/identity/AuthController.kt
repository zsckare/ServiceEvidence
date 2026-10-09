package com.serviceevidence.backend.identity

import com.serviceevidence.backend.tenants.TenantRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class LoginRequest(@field:Email val email: String, @field:NotBlank val password: String)
data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class TokenPair(val accessToken: String, val refreshToken: String, val tokenType: String = "Bearer")
data class TenantContextRequest(val tenantId: UUID)
data class UserView(val id: UUID, val email: String, val fullName: String, val platformAdmin: Boolean)

@RestController @RequestMapping("/api/v1/auth")
class AuthController(
 private val users: UserRepository, private val memberships: MembershipRepository,
 private val tenants: TenantRepository, private val passwords: PasswordEncoder,
 private val tokens: TokenService, private val audits: AuditRepository
) {
 @PostMapping("/login") @Transactional
 fun login(@Valid @RequestBody req: LoginRequest): TokenPair {
  val user = users.findByEmail(req.email.trim().lowercase())
  if (user == null || !user.enabled || !passwords.matches(req.password, user.passwordHash))
   throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
  audits.save(SecurityAudit(actorUserId = user.id, action = "LOGIN"))
  return TokenPair(tokens.access(user), tokens.refresh(user))
 }
 @PostMapping("/refresh") @Transactional
 fun refresh(@Valid @RequestBody req: RefreshRequest): TokenPair {
  val session = tokens.rotate(req.refreshToken)
  val user = users.findById(session.userId).orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
  if (!user.enabled) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
  // EN: Context must be explicitly selected again. ES: El contexto se vuelve a seleccionar explícitamente.
  return TokenPair(tokens.access(user), tokens.refresh(user))
 }
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
 fun logout(@RequestBody req: RefreshRequest) = tokens.revoke(req.refreshToken)
 @GetMapping("/me")
 fun me(@AuthenticationPrincipal jwt: Jwt): UserView {
  val user = users.findById(UUID.fromString(jwt.subject)).orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
  return UserView(user.id, user.email, user.fullName, user.platformAdmin)
 }
 @GetMapping("/available-tenants")
 fun available(@AuthenticationPrincipal jwt: Jwt): List<UUID> {
  val user = current(jwt)
  return if (user.platformAdmin) tenants.findAll().filter { it.active }.map { it.id }
   else memberships.findAllByUserIdAndEnabledTrue(user.id).mapNotNull { m -> tenants.findById(m.tenantId).orElse(null)?.takeIf { it.active }?.id }
 }
 @PostMapping("/tenant-context") @Transactional
 fun context(@AuthenticationPrincipal jwt: Jwt, @RequestBody req: TenantContextRequest): Map<String, String> {
  val user = current(jwt)
  val tenant = tenants.findById(req.tenantId).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
  if (!tenant.active) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant inactive")
  val membership = memberships.findByUserIdAndTenantId(user.id, tenant.id)?.takeIf { it.enabled }
  if (!user.platformAdmin && membership == null) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  audits.save(SecurityAudit(actorUserId = user.id, tenantId = tenant.id, action = "TENANT_CONTEXT_SELECTED"))
  return mapOf("accessToken" to tokens.access(user, tenant.id, membership?.role))
 }
 private fun current(jwt: Jwt): PlatformUser = users.findById(UUID.fromString(jwt.subject))
  .orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }.also {
   if (!it.enabled) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
  }
}
