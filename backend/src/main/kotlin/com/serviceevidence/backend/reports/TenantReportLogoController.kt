package com.serviceevidence.backend.reports

import com.serviceevidence.backend.shared.tenant.TenantAccess
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

/** EN: Tenant-authorized branding API. ES: API de marca con autorización por empresa. */
@RestController
@RequestMapping("/api/v1/settings/report-logo")
class TenantReportLogoController(private val access: TenantAccess, private val logos: TenantReportLogoService) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@AuthenticationPrincipal jwt: Jwt, @RequestParam("file") file: MultipartFile): ResponseEntity<Void> {
        val tenant = access.require(jwt, "TENANT_ADMIN")
        if (file.contentType !in setOf("image/png", "image/jpeg"))
            throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        logos.save(tenant, file.bytes)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(produces = [MediaType.IMAGE_PNG_VALUE])
    fun download(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<ByteArray> {
        val tenant = access.require(jwt, "TENANT_ADMIN", "SUPERVISOR")
        val bytes = logos.readBytes(tenant) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("Cache-Control", "private, no-store").body(bytes)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt) { logos.delete(access.require(jwt, "TENANT_ADMIN")) }
}
