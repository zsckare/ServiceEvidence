package com.serviceevidence.backend.shared.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

/** EN: JWT signatures are validated for every request. ES: Se verifica la firma JWT en cada petición. */
@Configuration @EnableWebSecurity @EnableMethodSecurity
class SecurityConfig {
 @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
 @Bean fun jwtEncoder(@Value("\${security.jwt.secret}") secret: String): JwtEncoder =
  NimbusJwtEncoder(ImmutableSecret<SecurityContext>(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")))
 @Bean fun jwtDecoder(@Value("\${security.jwt.secret}") secret: String): JwtDecoder =
  NimbusJwtDecoder.withSecretKey(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
   .macAlgorithm(MacAlgorithm.HS256).build()
 @Bean fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
  val authorities = JwtGrantedAuthoritiesConverter().apply { setAuthoritiesClaimName("roles"); setAuthorityPrefix("ROLE_") }
  val converter = JwtAuthenticationConverter().apply { setJwtGrantedAuthoritiesConverter(authorities) }
  return http.csrf { it.disable() }
   .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
   .authorizeHttpRequests {
    it.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
     .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
     .anyRequest().authenticated()
   }.oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(converter) } }
   .httpBasic { it.disable() }.formLogin { it.disable() }.build()
 }
}
