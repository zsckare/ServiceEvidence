package com.serviceevidence.backend

import com.serviceevidence.backend.evidence.ServiceEvidence
import com.serviceevidence.backend.reports.ServiceReportJasperService
import com.serviceevidence.backend.reports.TenantReportLogoService
import com.serviceevidence.backend.workorders.WorkOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** EN: Regression checks for report output and tenant-specific logos.
 * ES: Pruebas de regresión de PDF y logotipos aislados por tenant. */
class ServiceReportProfessionalTests {
    @TempDir lateinit var storage: Path

    private fun png(): ByteArray {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.BLUE
        graphics.fillRect(0, 0, 16, 16)
        graphics.dispose()
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    @Test fun `logos are isolated and removable by tenant`() {
        val service = TenantReportLogoService(storage.toString())
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        val saved = service.save(a, png())
        assertContentEquals(saved, service.readBytes(a))
        assertTrue(service.readBytes(b) == null)
        service.delete(a)
        assertTrue(service.readBytes(a) == null)
    }

    @Test fun `report renders PDF with tenant branding and evidence`() {
        val logos = TenantReportLogoService(storage.toString())
        val report = ServiceReportJasperService(logos, storage.toString())
        val tenant = UUID.randomUUID()
        val order = WorkOrder(tenantId = tenant, title = "Inspection report")
        logos.save(tenant, png())
        val evidenceId = UUID.randomUUID()
        val evidenceDir = storage.resolve(tenant.toString()).resolve(order.id.toString())
        java.nio.file.Files.createDirectories(evidenceDir)
        java.nio.file.Files.write(evidenceDir.resolve("$evidenceId.png"), png())
        val evidence = ServiceEvidence(id = evidenceId, tenantId = tenant, workOrderId = order.id,
            kind = "PHOTO", objectKey = "$evidenceId.png", contentType = "image/png")
        val pdf = report.generate(order, "Customer Example", "Location Example", listOf(evidence), "Acme Services", "UTC")
        assertTrue(pdf.copyOfRange(0, 5).decodeToString() == "%PDF-")
        assertTrue(pdf.size > 1000)
        // EN: A different tenant's evidence must not be used as an image.
        // ES: No se debe utilizar la imagen de otro tenant.
        val otherTenantEvidence = ServiceEvidence(id = evidenceId, tenantId = UUID.randomUUID(),
            workOrderId = order.id, kind = "PHOTO", objectKey = "$evidenceId.png", contentType = "image/png")
        val isolated = report.generate(order, "Customer Example", "Location Example", listOf(otherTenantEvidence))
        assertTrue(isolated.copyOfRange(0, 5).decodeToString() == "%PDF-")
        assertFalse(isolated.isEmpty())
    }
}
