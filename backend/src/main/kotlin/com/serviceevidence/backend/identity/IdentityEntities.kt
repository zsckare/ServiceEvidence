package com.serviceevidence.backend.identity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** EN: Global identity, independent of tenant membership. ES: Identidad global independiente del tenant. */
@Entity @Table(name = "platform_users")
class PlatformUser(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(nullable = false, unique = true) var email: String = "",
 @Column(name = "password_hash", nullable = false) var passwordHash: String = "",
 @Column(name = "full_name", nullable = false) var fullName: String = "",
 @Column(name = "platform_admin", nullable = false) var platformAdmin: Boolean = false,
 @Column(nullable = false) var enabled: Boolean = true,
 @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now()
)

@Entity @Table(name = "tenant_memberships")
class TenantMembership(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name = "tenant_id", nullable = false) var tenantId: UUID = UUID.randomUUID(),
 @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
 @Column(nullable = false) var role: String = "TECHNICIAN",
 @Column(nullable = false) var enabled: Boolean = true,
 @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now()
)

@Entity @Table(name = "refresh_sessions")
class RefreshSession(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name = "user_id", nullable = false) var userId: UUID = UUID.randomUUID(),
 @Column(name = "token_hash", nullable = false) var tokenHash: String = "",
 @Column(name = "expires_at", nullable = false) var expiresAt: Instant = Instant.now(),
 @Column(nullable = false) var revoked: Boolean = false,
 @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now()
)

@Entity @Table(name = "security_audit")
class SecurityAudit(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name = "actor_user_id") var actorUserId: UUID? = null,
 @Column(name = "tenant_id") var tenantId: UUID? = null,
 @Column(nullable = false) var action: String = "",
 @Column(name = "target_id") var targetId: String? = null,
 @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now()
)
