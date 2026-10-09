package com.serviceevidence.backend.identity

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

/** EN: Issues short-lived JWTs and opaque rotating refresh tokens. ES: Emite JWT cortos y refresh tokens rotativos. */
@Service
class TokenService(
 private val encoder: JwtEncoder, private val refreshRepo: RefreshRepository,
 @Value("\${security.jwt.access-minutes:15}") private val accessMinutes: Long,
 @Value("\${security.jwt.refresh-days:14}") private val refreshDays: Long
) {
 private val random = SecureRandom()
 fun access(user: PlatformUser, tenantId: UUID? = null, role: String? = null): String {
  val now = Instant.now()
  val roles = if (tenantId == null) listOfNotNull(if (user.platformAdmin) "SUPER_ADMIN" else null) else
   listOfNotNull(role, if (user.platformAdmin) "SUPER_ADMIN" else null)
  val claims = JwtClaimsSet.builder().issuer("serviceevidence").subject(user.id.toString())
   .issuedAt(now).expiresAt(now.plus(accessMinutes, ChronoUnit.MINUTES))
   .claim("roles", roles).claim("token_type", "access")
  if (tenantId != null) claims.claim("tenant_id", tenantId.toString())
  return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).tokenValue
 }
 fun refresh(user: PlatformUser): String {
  val bytes = ByteArray(48).also(random::nextBytes)
  val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  refreshRepo.save(RefreshSession(userId = user.id, tokenHash = hash(raw), expiresAt = Instant.now().plus(refreshDays, ChronoUnit.DAYS)))
  return raw
 }
 fun rotate(raw: String): RefreshSession {
  val session = refreshRepo.findByTokenHash(hash(raw)) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")
  if (session.revoked || !session.expiresAt.isAfter(Instant.now())) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired refresh token")
  session.revoked = true
  return refreshRepo.save(session)
 }
 fun revoke(raw: String) { refreshRepo.findByTokenHash(hash(raw))?.let { it.revoked = true; refreshRepo.save(it) } }
 private fun hash(raw: String): String = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
}
