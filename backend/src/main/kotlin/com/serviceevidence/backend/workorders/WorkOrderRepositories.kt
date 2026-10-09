package com.serviceevidence.backend.workorders

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WorkOrderRepository : JpaRepository<WorkOrder, UUID> {
 fun findByTenantIdAndId(tenantId: UUID, id: UUID): WorkOrder?
 fun findByTenantId(tenantId: UUID, pageable: Pageable): Page<WorkOrder>
 fun findByTenantIdAndStatus(tenantId: UUID, status: String, pageable: Pageable): Page<WorkOrder>
 fun countByTenantIdAndStatus(tenantId: UUID, status: String): Long
}
interface WorkOrderAssignmentRepository : JpaRepository<WorkOrderAssignment, UUID> {
 fun findAllByTenantIdAndWorkOrderId(tenantId: UUID, workOrderId: UUID): List<WorkOrderAssignment>
 fun findAllByTenantIdAndTechnicianId(tenantId: UUID, technicianId: UUID): List<WorkOrderAssignment>
 fun existsByTenantIdAndWorkOrderIdAndTechnicianId(tenantId: UUID, workOrderId: UUID, technicianId: UUID): Boolean
 fun deleteByTenantIdAndWorkOrderIdAndTechnicianId(tenantId: UUID, workOrderId: UUID, technicianId: UUID): Long
}
interface WorkOrderEventRepository : JpaRepository<WorkOrderEvent, UUID> {
 fun findAllByTenantIdAndWorkOrderIdOrderByCreatedAtAsc(tenantId: UUID, workOrderId: UUID): List<WorkOrderEvent>
}
