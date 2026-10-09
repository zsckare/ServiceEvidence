
package com.serviceevidence.backend.reports

import com.serviceevidence.backend.evidence.ServiceEvidence
import com.serviceevidence.backend.workorders.WorkOrder
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.Image
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.imageio.ImageIO

/**
 * EN: Public JavaBean-compatible row used by JasperReports.
 * ES: Fila pública compatible con JavaBeans utilizada por JasperReports.
 *
 * EN: This is a top-level class so Apache Commons BeanUtils can
 * access its generated public getters through reflection.
 *
 * ES: Se declara fuera del servicio para que Apache Commons BeanUtils
 * pueda acceder a sus getters públicos mediante reflexión.
 */
data class EvidenceRow(
    val kind: String,
    val id: String,
    val notes: String,
    val createdAt: String,
    val image: Image? = null
)

/**
 * EN: Generates professional JasperReports PDFs for tenant-scoped
 * work orders, including photographic evidence and signatures.
 *
 * ES: Genera documentos PDF profesionales con JasperReports para
 * órdenes de trabajo, incluyendo fotografías y firmas.
 */
@Service
class ServiceReportJasperService(
    @Value("\${serviceevidence.storage.path:./private-evidence}")
    private val storagePath: String
) {

    /**
     * EN: Compiles the JRXML template once, using thread-safe lazy initialization.
     * ES: Compila la plantilla JRXML una sola vez de manera segura.
     */
    private val template: JasperReport by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val resource = javaClass.getResourceAsStream(
            "/reports/service_report.jrxml"
        ) ?: error(
            "Missing Jasper template: /reports/service_report.jrxml"
        )

        resource.use {
            JasperCompileManager.compileReport(it)
        }
    }

    /**
     * EN: Generates the PDF for an authorized work order.
     * ES: Genera el PDF de una orden de trabajo autorizada.
     *
     * EN: Tenant authorization must be validated by the calling controller.
     * ES: La autorización del tenant debe validarse en el controlador.
     */
    fun generate(
        order: WorkOrder,
        customerName: String,
        locationName: String,
        evidence: List<ServiceEvidence>,
        companyName: String = "ServiceEvidence",
        timezone: String = "UTC"
    ): ByteArray {

        // EN: Resolve the tenant's timezone, falling back to UTC.
        // ES: Obtiene la zona horaria del tenant; usa UTC como respaldo.
        val zone = runCatching {
            ZoneId.of(timezone)
        }.getOrDefault(ZoneOffset.UTC)

        // EN: Parameters displayed in the JRXML report.
        // ES: Parámetros que utiliza la plantilla JRXML.
        val parameters: Map<String, Any> = mapOf(
            "COMPANY" to companyName.ifBlank { "ServiceEvidence" },
            "ORDER_ID" to order.id.toString(),
            "TITLE" to order.title,
            "DESCRIPTION" to (order.description ?: "—"),
            "CUSTOMER" to customerName,
            "LOCATION" to locationName,
            "STATUS" to order.status,
            "PRIORITY" to order.priority,
            "CREATED" to format(order.createdAt, zone),
            "SCHEDULED" to format(order.scheduledAt, zone),
            "STARTED" to format(order.startedAt, zone),
            "COMPLETED" to format(order.completedAt, zone),
            "EVIDENCE_COUNT" to evidence.size.toString(),
            "TIMEZONE" to zone.id
        )

        /**
         * EN: Convert evidence entities into public JasperReports beans.
         * ES: Convierte las evidencias en objetos públicos para JasperReports.
         */
        val rows = evidence.map { item ->
            EvidenceRow(
                kind = item.kind,
                id = item.id.toString(),
                notes = item.notes ?: "",
                createdAt = format(item.createdAt, zone),
                image = loadImage(
                    tenantId = order.tenantId,
                    orderId = order.id,
                    item = item
                )
            )
        }.ifEmpty {
            listOf(
                EvidenceRow(
                    kind = "—",
                    id = "No evidence registered",
                    notes = "",
                    createdAt = "",
                    image = null
                )
            )
        }

        // EN: Fill the report using the evidence collection.
        // ES: Rellena el reporte con la colección de evidencias.
        val filled = JasperFillManager.fillReport(
            template,
            parameters,
            JRBeanCollectionDataSource(rows)
        )

        // EN: Export the completed report as PDF bytes.
        // ES: Exporta el reporte como un arreglo de bytes PDF.
        return JasperExportManager.exportReportToPdf(filled)
    }

    /**
     * EN: Loads an image only from the authorized tenant and work order.
     * ES: Carga imágenes únicamente del tenant y la orden autorizados.
     *
     * EN: Unsupported or missing images are represented as null.
     * ES: Las imágenes no disponibles o incompatibles se representan con null.
     */
    private fun loadImage(
        tenantId: UUID,
        orderId: UUID,
        item: ServiceEvidence
    ): Image? {

        // EN: Verify that the evidence belongs to the expected tenant/order.
        // ES: Verifica que la evidencia pertenezca al tenant y orden correctos.
        if (item.tenantId != tenantId || item.workOrderId != orderId) {
            return null
        }

        if (item.kind !in setOf("PHOTO", "SIGNATURE")) {
            return null
        }

        if (item.contentType !in setOf(
                "image/png",
                "image/jpeg",
                "image/webp"
            )
        ) {
            return null
        }

        // EN: Prevent unsafe file names and path traversal.
        // ES: Impide nombres inseguros y recorridos de directorios.
        if (!item.objectKey.matches(
                Regex("[0-9a-f-]{36}\\.(jpg|jpeg|png|webp)")
            )
        ) {
            return null
        }

        val root = Path.of(storagePath)
            .toAbsolutePath()
            .normalize()

        val folder = root
            .resolve(tenantId.toString())
            .resolve(orderId.toString())
            .normalize()

        val path = folder
            .resolve(item.objectKey)
            .normalize()

        if (!path.startsWith(folder) || !Files.isRegularFile(path)) {
            return null
        }

        /**
         * EN: Limit file size before loading into memory.
         * ES: Limita el tamaño antes de cargar el archivo en memoria.
         */
        return runCatching {
            if (Files.size(path) > 20_000_000L) {
                return null
            }

            ImageIO.read(
                ByteArrayInputStream(Files.readAllBytes(path))
            )
        }.getOrNull()
    }

    /**
     * EN: Formats timestamps in the tenant's timezone.
     * ES: Formatea las fechas según la zona horaria del tenant.
     */
    private fun format(
        instant: Instant?,
        zone: ZoneId
    ): String {
        return instant?.let {
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(zone)
                .format(it)
        } ?: "—"
    }
}
