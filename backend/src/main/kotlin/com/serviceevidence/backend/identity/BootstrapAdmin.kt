package com.serviceevidence.backend.identity

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** EN: Initial global admin is provisioned only from explicit environment secrets.
 * ES: El administrador global inicial se crea solo mediante secretos de entorno explícitos.
 */
@Component
class BootstrapAdmin(
 private val users: UserRepository, private val passwords: PasswordEncoder,
 @Value("\${bootstrap.admin.email:}") private val email: String,
 @Value("\${bootstrap.admin.password:}") private val password: String
): ApplicationRunner {
 @Transactional override fun run(args: ApplicationArguments) {
  if (email.isBlank() || password.isBlank()) return
  require(password.length >= 16) { "Bootstrap password must have at least 16 characters" }
  if (!users.existsByPlatformAdminTrue()) {
   require(users.findByEmail(email.trim().lowercase()) == null) { "Bootstrap email is already in use" }
   users.save(PlatformUser(email=email.trim().lowercase(),fullName="Platform Administrator",passwordHash=requireNotNull(passwords.encode(password)) { "Password encoder returned null" },platformAdmin=true))
  }
 }
}
