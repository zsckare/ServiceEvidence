package com.serviceevidence.backend.tenants

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantRepository : JpaRepository<Tenant, UUID> {
 fun existsByCode(code: String): Boolean
}
