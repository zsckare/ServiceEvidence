package com.serviceevidence.backend.evidence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** EN: Evidence metadata; binary uploads are deliberately not stored in the database. ES: Metadatos de evidencia. */
@Entity @Table(name="service_evidence")
class ServiceEvidence(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name="tenant_id",nullable=false) var tenantId: UUID = UUID.randomUUID(),
 @Column(name="work_order_id",nullable=false) var workOrderId: UUID = UUID.randomUUID(),
 @Column(name="uploaded_by",nullable=false) var uploadedBy: UUID = UUID.randomUUID(),
 var kind: String = "PHOTO",
 @Column(name="object_key",nullable=false) var objectKey: String = "",
 @Column(name="content_type",nullable=false) var contentType: String = "",
 @Column(name="byte_size",nullable=false) var byteSize: Long = 0,
 @Column(name="sha256",nullable=false) var sha256: String = "",
 @Column(columnDefinition="text") var notes: String? = null,
 @Column(name="created_at",nullable=false) var createdAt: Instant = Instant.now()
)
