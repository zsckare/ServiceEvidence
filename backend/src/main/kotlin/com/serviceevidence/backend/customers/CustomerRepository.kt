package com.serviceevidence.backend.customers

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomerRepository : JpaRepository<Customer, UUID> {
 fun findByTenantIdAndId(tenantId: UUID, id: UUID): Customer?
 fun findByTenantId(tenantId: UUID, pageable: Pageable): Page<Customer>
 fun findByTenantIdAndNameContainingIgnoreCase(tenantId: UUID, name: String, pageable: Pageable): Page<Customer>
}
