package com.serviceevidence.backend.reports

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.imageio.ImageIO

/** EN: Private tenant-branded logo storage. ES: Almacenamiento privado de logotipos por empresa. */
@Service
class TenantReportLogoService(
    @Value("\${serviceevidence.storage.path:./private-evidence}") private val storagePath: String
) {
    private fun logoPath(tenantId: UUID): Path = Path.of(storagePath).toAbsolutePath().normalize()
        .resolve(tenantId.toString()).resolve("branding").resolve("logo.png")

    /** EN: Store only decoded and re-encoded PNG pixels, not untrusted original bytes.
     * ES: Guarda únicamente píxeles decodificados y recodificados como PNG. */
    fun save(tenantId: UUID, bytes: ByteArray): ByteArray {
        if (bytes.isEmpty() || bytes.size > 2_000_000) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo must be 1-2 MB")
        val image = try { ImageIO.read(ByteArrayInputStream(bytes)) } catch (_: Exception) { null }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid logo image")
        if (image.width !in 1..2000 || image.height !in 1..2000)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo dimensions exceed 2000 px")
        val target = logoPath(tenantId)
        Files.createDirectories(target.parent)
        val temp = Files.createTempFile(target.parent, "logo-", ".tmp")
        try {
            Files.newOutputStream(temp).use { stream ->
                if (!ImageIO.write(image, "png", stream)) throw IllegalStateException("PNG encoder unavailable")
            }
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temp) }
        return Files.readAllBytes(target)
    }

    fun readBytes(tenantId: UUID): ByteArray? = logoPath(tenantId).takeIf { Files.isRegularFile(it) }?.let { Files.readAllBytes(it) }
    fun readImage(tenantId: UUID): BufferedImage? = readBytes(tenantId)?.let {
        runCatching { ImageIO.read(ByteArrayInputStream(it)) }.getOrNull()
    }
    fun delete(tenantId: UUID) { Files.deleteIfExists(logoPath(tenantId)) }
}
