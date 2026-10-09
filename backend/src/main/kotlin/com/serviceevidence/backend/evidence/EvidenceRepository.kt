package com.serviceevidence.backend.evidence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EvidenceRepository: JpaRepository<ServiceEvidence, UUID> {
 fun findAllByTenantIdAndWorkOrderIdOrderByCreatedAtDesc(tenantId: UUID,workOrderId: UUID): List<ServiceEvidence>
 fun findByTenantIdAndId(tenantId: UUID,id: UUID): ServiceEvidence?
}
