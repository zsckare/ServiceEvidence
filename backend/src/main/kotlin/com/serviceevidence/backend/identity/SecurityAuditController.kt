package com.serviceevidence.backend.identity

import com.serviceevidence.backend.shared.tenant.TenantAccess
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

/** EN: Read-only, tenant-scoped security audit feed. ES: Auditoría de seguridad limitada al tenant. */
data class AuditEntry(val id: UUID, val actorUserId: UUID?, val action: String, val targetId: String?, val createdAt: Instant)

@RestController
@RequestMapping("/api/v1/security/audit")
class SecurityAuditController(private val access: TenantAccess, private val jdbc: JdbcTemplate) {
 @GetMapping
 fun list(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "0") page: Int,
          @RequestParam(defaultValue = "30") size: Int): List<AuditEntry> {
  val tenantId = access.require(jwt, "TENANT_ADMIN")
  if (page < 0 || size !in 1..100 || page > 100000)
   throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination")
  return jdbc.query(
   """SELECT id, actor_user_id, action, target_id, created_at FROM security_audit
      WHERE tenant_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?""",
   { rs, _ -> AuditEntry(UUID.fromString(rs.getString("id")),
    rs.getString("actor_user_id")?.let(UUID::fromString), rs.getString("action"),
    rs.getString("target_id"), rs.getTimestamp("created_at").toInstant()) },
   tenantId, size, page.toLong() * size
  )
 }
}
