package com.serviceevidence.backend.workorders

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** EN: Tenant-owned service job. ES: Trabajo de servicio perteneciente a una empresa. */
@Entity @Table(name="work_orders")
class WorkOrder(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name="tenant_id", nullable=false) var tenantId: UUID = UUID.randomUUID(),
 @Column(name="customer_id", nullable=false) var customerId: UUID = UUID.randomUUID(),
 @Column(name="location_id", nullable=false) var locationId: UUID = UUID.randomUUID(),
 var title: String = "",
 @Column(columnDefinition="text") var description: String? = null,
 var priority: String = "MEDIUM",
 var status: String = "DRAFT",
 @Column(name="scheduled_at") var scheduledAt: Instant? = null,
 @Column(name="started_at") var startedAt: Instant? = null,
 @Column(name="completed_at") var completedAt: Instant? = null,
 @Column(name="created_by", nullable=false) var createdBy: UUID = UUID.randomUUID(),
 @Column(name="created_at", nullable=false) var createdAt: Instant = Instant.now(),
 @Column(name="updated_at", nullable=false) var updatedAt: Instant = Instant.now()
)

/** EN: Explicit assignment table. ES: Asignaciones independientes de la orden. */
@Entity @Table(name="work_order_assignments")
class WorkOrderAssignment(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name="tenant_id", nullable=false) var tenantId: UUID = UUID.randomUUID(),
 @Column(name="work_order_id", nullable=false) var workOrderId: UUID = UUID.randomUUID(),
 @Column(name="technician_id", nullable=false) var technicianId: UUID = UUID.randomUUID(),
 @Column(name="is_primary", nullable=false) var primary: Boolean = false
)

@Entity @Table(name="work_order_events")
class WorkOrderEvent(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name="tenant_id", nullable=false) var tenantId: UUID = UUID.randomUUID(),
 @Column(name="work_order_id", nullable=false) var workOrderId: UUID = UUID.randomUUID(),
 @Column(name="actor_id", nullable=false) var actorId: UUID = UUID.randomUUID(),
 var action: String = "",
 @Column(columnDefinition="text") var details: String? = null,
 @Column(name="created_at", nullable=false) var createdAt: Instant = Instant.now()
)
