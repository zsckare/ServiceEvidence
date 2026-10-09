package com.serviceevidence.backend.locations

import com.serviceevidence.backend.customers.CustomerRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

data class LocationInput(
 @field:NotBlank @field:Size(max=180) val name: String,
 @field:NotBlank @field:Size(max=255) val addressLine1: String,
 @field:Size(max=255) val addressLine2: String? = null,
 @field:NotBlank @field:Size(max=120) val city: String,
 @field:Size(max=120) val state: String? = null,
 @field:Size(max=30) val postalCode: String? = null,
 @field:NotBlank @field:Size(max=100) val country: String = "MX",
 @field:Size(max=160) val contactName: String? = null,
 @field:Size(max=50) val contactPhone: String? = null,
 val notes: String? = null,
 val active: Boolean = true
)
data class LocationView(val id: UUID,val customerId: UUID,val name: String,val addressLine1: String,val addressLine2: String?,val city: String,val state: String?,val postalCode: String?,val country: String,val contactName: String?,val contactPhone: String?,val notes: String?,val active: Boolean)
private fun ServiceLocation.view()=LocationView(id,customerId,name,addressLine1,addressLine2,city,state,postalCode,country,contactName,contactPhone,notes,active)

/** EN: Sites always resolved by both tenant and customer. ES: Sedes siempre filtradas por tenant y cliente. */
@RestController @RequestMapping("/api/v1/customers/{customerId}/locations")
class LocationsController(private val access: TenantAccess,private val customers: CustomerRepository,private val locations: LocationRepository) {
 private fun verify(tenant: UUID, customerId: UUID) {
  if (customers.findByTenantIdAndId(tenant,customerId)==null) throw ResponseStatusException(HttpStatus.NOT_FOUND)
 }
 @GetMapping
 fun list(@AuthenticationPrincipal jwt: Jwt,@PathVariable customerId: UUID,@RequestParam(defaultValue="0") page: Int,@RequestParam(defaultValue="20") size: Int): Page<LocationView> {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  verify(tenant,customerId)
  if(page<0 || size !in 1..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
  return locations.findByTenantIdAndCustomerId(tenant,customerId,PageRequest.of(page,size,Sort.by("name"))).map { it.view() }
 }
 @GetMapping("/{id}")
 fun get(@AuthenticationPrincipal jwt: Jwt,@PathVariable customerId: UUID,@PathVariable id: UUID): LocationView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  verify(tenant,customerId)
  val location=locations.findByTenantIdAndId(tenant,id)
  if(location==null || location.customerId!=customerId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  return location.view()
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
 fun create(@AuthenticationPrincipal jwt: Jwt,@PathVariable customerId: UUID,@Valid @RequestBody input: LocationInput): LocationView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR")
  verify(tenant,customerId)
  return locations.save(ServiceLocation(tenantId=tenant,customerId=customerId).applyInput(input)).view()
 }
 @PutMapping("/{id}") @Transactional
 fun update(@AuthenticationPrincipal jwt: Jwt,@PathVariable customerId: UUID,@PathVariable id: UUID,@Valid @RequestBody input: LocationInput): LocationView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR")
  verify(tenant,customerId)
  val location=locations.findByTenantIdAndId(tenant,id)
  if(location==null || location.customerId!=customerId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  return locations.save(location.applyInput(input)).view()
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 fun delete(@AuthenticationPrincipal jwt: Jwt,@PathVariable customerId: UUID,@PathVariable id: UUID) {
  val tenant=access.require(jwt,"TENANT_ADMIN")
  verify(tenant,customerId)
  val location=locations.findByTenantIdAndId(tenant,id)
  if(location==null || location.customerId!=customerId) throw ResponseStatusException(HttpStatus.NOT_FOUND)
  location.active=false;location.updatedAt=Instant.now();locations.save(location)
 }
}
private fun ServiceLocation.applyInput(input: LocationInput): ServiceLocation = apply {
 name=input.name.trim();addressLine1=input.addressLine1.trim();addressLine2=input.addressLine2?.trim()
 city=input.city.trim();state=input.state?.trim();postalCode=input.postalCode?.trim();country=input.country.trim()
 contactName=input.contactName?.trim();contactPhone=input.contactPhone?.trim();notes=input.notes
 active=input.active;updatedAt=Instant.now()
}
