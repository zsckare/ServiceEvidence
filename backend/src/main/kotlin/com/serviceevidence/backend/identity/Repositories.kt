package com.serviceevidence.backend.identity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<PlatformUser, UUID> {
 fun findByEmail(email: String): PlatformUser?
 fun existsByPlatformAdminTrue(): Boolean
}
interface MembershipRepository : JpaRepository<TenantMembership, UUID> {
 fun findByUserIdAndTenantId(userId: UUID, tenantId: UUID): TenantMembership?
 fun findAllByUserIdAndEnabledTrue(userId: UUID): List<TenantMembership>
 fun findAllByTenantId(tenantId: UUID): List<TenantMembership>
}
interface RefreshRepository : JpaRepository<RefreshSession, UUID> {
 fun findByTokenHash(tokenHash: String): RefreshSession?
}
interface AuditRepository : JpaRepository<SecurityAudit, UUID>
