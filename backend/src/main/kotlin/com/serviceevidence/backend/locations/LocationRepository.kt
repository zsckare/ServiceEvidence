package com.serviceevidence.backend.locations

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationRepository : JpaRepository<ServiceLocation, UUID> {
 fun findByTenantIdAndId(tenantId: UUID, id: UUID): ServiceLocation?
 fun findByTenantIdAndCustomerId(tenantId: UUID, customerId: UUID, pageable: Pageable): Page<ServiceLocation>
 fun existsByTenantIdAndCustomerId(tenantId: UUID, customerId: UUID): Boolean
}
