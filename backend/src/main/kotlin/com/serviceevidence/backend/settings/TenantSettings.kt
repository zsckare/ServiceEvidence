package com.serviceevidence.backend.settings

import com.serviceevidence.backend.shared.tenant.TenantAccess
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** EN: Per-tenant configuration, never shared across companies. ES: Configuración aislada por empresa. */
@Entity @Table(name="tenant_settings")
class TenantSettings(@Id @Column(name="tenant_id") var tenantId: UUID=UUID.randomUUID(),
 var displayName: String="", var supportEmail: String="", var timezone: String="UTC")
interface TenantSettingsRepository: org.springframework.data.jpa.repository.JpaRepository<TenantSettings,UUID>
data class SettingsInput(@field:Size(max=160) val displayName:String="",@field:Size(max=200) val supportEmail:String="",@field:Size(max=80) val timezone:String="UTC")
@RestController @RequestMapping("/api/v1/settings")
class TenantSettingsController(private val access:TenantAccess,private val repository:TenantSettingsRepository) {
 @GetMapping fun get(@AuthenticationPrincipal jwt:Jwt):SettingsInput {
  val tenant=access.require(jwt,"TENANT_ADMIN","SUPERVISOR")
  val s=repository.findById(tenant).orElse(null) ?: return SettingsInput()
  return SettingsInput(s.displayName,s.supportEmail,s.timezone)
 }
 @PutMapping @Transactional fun put(@AuthenticationPrincipal jwt:Jwt,@Valid @RequestBody input:SettingsInput):SettingsInput {
  val tenant=access.require(jwt,"TENANT_ADMIN")
  java.time.ZoneId.of(input.timezone)
  repository.save(TenantSettings(tenant,input.displayName,input.supportEmail,input.timezone))
  return input
 }
}
