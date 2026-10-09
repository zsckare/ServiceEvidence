package com.serviceevidence.backend.shared.tenant

import com.serviceevidence.backend.identity.MembershipRepository
import com.serviceevidence.backend.identity.UserRepository
import com.serviceevidence.backend.tenants.TenantRepository
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/** EN: Resolves tenant from signed JWT and rechecks membership. ES: Resuelve el tenant del JWT firmado y valida membresía. */
@Component
class TenantAccess(private val users: UserRepository, private val memberships: MembershipRepository, private val tenants: TenantRepository) {
 fun require(jwt: Jwt, vararg allowedRoles: String): UUID {
  val tenantId = jwt.getClaimAsString("tenant_id")?.let { runCatching { UUID.fromString(it) }.getOrNull() }
   ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Select a tenant")
  val userId = runCatching { UUID.fromString(jwt.subject) }.getOrNull()
   ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
  val actor = users.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED) }
  if (!actor.enabled) throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
  if (tenants.findById(tenantId).orElse(null)?.active != true) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  if (!actor.platformAdmin) {
   val membership = memberships.findByUserIdAndTenantId(userId, tenantId)
   if (membership?.enabled != true || membership.role !in allowedRoles.toSet()) throw ResponseStatusException(HttpStatus.FORBIDDEN)
  }
  return tenantId
 }
}
