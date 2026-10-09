package com.serviceevidence.backend.customers

import com.serviceevidence.backend.locations.LocationRepository
import com.serviceevidence.backend.shared.tenant.TenantAccess
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
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

data class CustomerInput(
 @field:NotBlank @field:Size(max=180) val name: String,
 @field:Size(max=220) val legalName: String? = null,
 @field:Size(max=50) val taxId: String? = null,
 @field:Email val email: String? = null,
 @field:Size(max=50) val phone: String? = null,
 val notes: String? = null,
 val active: Boolean = true
)
data class CustomerView(val id: UUID, val name: String, val legalName: String?, val taxId: String?, val email: String?, val phone: String?, val notes: String?, val active: Boolean)
fun Customer.view() = CustomerView(id,name,legalName,taxId,email,phone,notes,active)

/** EN: Tenant-scoped customer management. ES: Gestión de clientes aislada por empresa. */
@RestController @RequestMapping("/api/v1/customers")
class CustomersController(private val access: TenantAccess, private val repo: CustomerRepository, private val locations: LocationRepository) {
 @GetMapping
 fun list(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required=false) search: String?, @RequestParam(defaultValue="0") page: Int, @RequestParam(defaultValue="20") size: Int): Page<CustomerView> {
  val tenant = access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  if (page < 0 || size !in 1..100) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination")
  val pageable = PageRequest.of(page,size,Sort.by("name").ascending())
  return (if (search.isNullOrBlank()) repo.findByTenantId(tenant,pageable) else repo.findByTenantIdAndNameContainingIgnoreCase(tenant,search.trim(),pageable)).map { it.view() }
 }
 @GetMapping("/{id}")
 fun get(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID): CustomerView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR","TECHNICIAN")
  return (repo.findByTenantIdAndId(tenant,id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)).view()
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
 fun create(@AuthenticationPrincipal jwt: Jwt,@Valid @RequestBody input: CustomerInput): CustomerView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR")
  return repo.save(Customer(tenantId=tenant).applyInput(input)).view()
 }
 @PutMapping("/{id}") @Transactional
 fun update(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID,@Valid @RequestBody input: CustomerInput): CustomerView {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR")
  val customer=repo.findByTenantIdAndId(tenant,id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  return repo.save(customer.applyInput(input)).view()
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 fun delete(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: UUID) {
  val tenant=access.require(jwt,"TENANT_ADMIN")
  val customer=repo.findByTenantIdAndId(tenant,id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
  // EN: Soft delete keeps evidence and historical references. ES: Baja lógica para preservar historial.
  customer.active=false; customer.updatedAt=Instant.now(); repo.save(customer)
 }
}
private fun Customer.applyInput(input: CustomerInput): Customer = apply {
 name=input.name.trim(); legalName=input.legalName?.trim(); taxId=input.taxId?.trim()
 email=input.email?.trim(); phone=input.phone?.trim(); notes=input.notes; active=input.active; updatedAt=Instant.now()
}
