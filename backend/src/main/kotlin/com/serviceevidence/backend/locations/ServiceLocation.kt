package com.serviceevidence.backend.locations

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** EN: Physical service site; customer belongs to same tenant via DB constraint. ES: Sede física, con integridad por tenant. */
@Entity @Table(name="service_locations")
class ServiceLocation(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(name="tenant_id",nullable=false) var tenantId: UUID = UUID.randomUUID(),
 @Column(name="customer_id",nullable=false) var customerId: UUID = UUID.randomUUID(),
 var name: String = "",
 @Column(name="address_line1") var addressLine1: String = "",
 @Column(name="address_line2") var addressLine2: String? = null,
 var city: String = "",
 var state: String? = null,
 @Column(name="postal_code") var postalCode: String? = null,
 var country: String = "MX",
 @Column(name="contact_name") var contactName: String? = null,
 @Column(name="contact_phone") var contactPhone: String? = null,
 @Column(columnDefinition="text") var notes: String? = null,
 var active: Boolean = true,
 @Column(name="created_at") var createdAt: Instant = Instant.now(),
 @Column(name="updated_at") var updatedAt: Instant = Instant.now()
)
