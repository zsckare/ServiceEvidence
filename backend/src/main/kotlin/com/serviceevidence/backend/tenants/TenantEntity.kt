package com.serviceevidence.backend.tenants

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** EN: Tenant registry. ES: Registro de empresas aisladas. */
@Entity @Table(name = "tenants")
class Tenant(
 @Id var id: UUID = UUID.randomUUID(),
 @Column(nullable = false, unique = true) var code: String = "",
 @Column(nullable = false) var name: String = "",
 @Column(nullable = false) var active: Boolean = true,
 @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now()
)
