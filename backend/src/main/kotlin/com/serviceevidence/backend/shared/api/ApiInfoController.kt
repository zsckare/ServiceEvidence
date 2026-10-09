package com.serviceevidence.backend.shared.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * EN: Protected example endpoint; JWT authentication will be added in the identity sprint.
 * ES: Endpoint protegido de ejemplo; agregaremos autenticación JWT en el sprint de identidad.
 */
@RestController
@RequestMapping("/api/v1")
class ApiInfoController {
    @GetMapping("/info")
    fun info(): Map<String, String> = mapOf("service" to "ServiceEvidence", "status" to "ready")
}
