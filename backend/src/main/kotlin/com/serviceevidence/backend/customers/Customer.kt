package com.serviceevidence.backend.customers

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** EN: Customer owned by a tenant. ES: Cliente perteneciente a un tenant. */
@Entity @Table(name = "customers")
class Customer(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name="tenant_id",nullable=false) var tenantId: UUID = UUID.randomUUID(),
 @Column(nullable=false) var name: String = "",
 @Column(name="legal_name") var legalName: String? = null,
 @Column(name="tax_id") var taxId: String? = null,
 var email: String? = null,
 var phone: String? = null,
 @Column(columnDefinition="text") var notes: String? = null,
 var active: Boolean = true,
 @Column(name="created_at") var createdAt: Instant = Instant.now(),
 @Column(name="updated_at") var updatedAt: Instant = Instant.now()
)
