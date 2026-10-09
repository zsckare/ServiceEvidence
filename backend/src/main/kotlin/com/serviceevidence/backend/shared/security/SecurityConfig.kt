package com.serviceevidence.backend.shared.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * EN: Secure defaults: only the health endpoint is public; business APIs require authentication.
 * ES: Valores seguros: solo el endpoint de salud es público; las APIs de negocio requieren autenticación.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() } // EN: Stateless API without cookie sessions. ES: API sin sesiones por cookies.
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .anyRequest().authenticated()
        }
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        .build()
}
