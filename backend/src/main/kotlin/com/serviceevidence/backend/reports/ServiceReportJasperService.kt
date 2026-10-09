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

/** EN: Public bean for JasperReports reflection. ES: Bean público para reflexión de JasperReports. */
data class EvidenceRow(
    val kind: String, val id: String, val notes: String,
    val createdAt: String, val image: Image? = null
)

/** EN: Tenant-scoped professional service report. ES: Reporte profesional aislado por empresa. */
@Service
class ServiceReportJasperService(
    private val logos: TenantReportLogoService,
    @Value("\${serviceevidence.storage.path:./private-evidence}") private val storagePath: String
) {
    private val template: JasperReport by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val resource = javaClass.getResourceAsStream("/reports/service_report.jrxml")
            ?: error("Missing Jasper template")
        resource.use { JasperCompileManager.compileReport(it) }
    }

    fun generate(order: WorkOrder, customerName: String, locationName: String,
                 evidence: List<ServiceEvidence>, companyName: String = "ServiceEvidence",
                 timezone: String = "UTC"): ByteArray {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)
        val parameters: Map<String, Any?> = mapOf(
            "COMPANY" to companyName.ifBlank { "ServiceEvidence" },
            "LOGO" to logos.readImage(order.tenantId),
            "ORDER_ID" to order.id.toString(), "TITLE" to order.title,
            "DESCRIPTION" to (order.description ?: "—"),
            "CUSTOMER" to customerName, "LOCATION" to locationName,
            "STATUS" to order.status, "PRIORITY" to order.priority,
            "CREATED" to format(order.createdAt, zone),
            "SCHEDULED" to format(order.scheduledAt, zone),
            "STARTED" to format(order.startedAt, zone),
            "COMPLETED" to format(order.completedAt, zone),
            "EVIDENCE_COUNT" to evidence.size.toString(), "TIMEZONE" to zone.id
        )
        val rows = evidence.filter { it.tenantId == order.tenantId && it.workOrderId == order.id }
            .map { item -> EvidenceRow(item.kind, item.id.toString(), item.notes ?: "",
                format(item.createdAt, zone), loadImage(order, item)) }
            .ifEmpty { listOf(EvidenceRow("—", "No evidence registered", "", "")) }
        val filled = JasperFillManager.fillReport(template, parameters, JRBeanCollectionDataSource(rows))
        return JasperExportManager.exportReportToPdf(filled)
    }

    private fun loadImage(order: WorkOrder, item: ServiceEvidence): Image? {
        if (item.tenantId != order.tenantId || item.workOrderId != order.id ||
            item.kind !in setOf("PHOTO", "SIGNATURE") ||
            item.contentType !in setOf("image/png", "image/jpeg", "image/webp") ||
            !item.objectKey.matches(Regex("[0-9a-fA-F-]{36}\\.(png|jpg|jpeg|webp)"))) return null
        val root = Path.of(storagePath).toAbsolutePath().normalize()
        val dir = root.resolve(order.tenantId.toString()).resolve(order.id.toString()).normalize()
        val path = dir.resolve(item.objectKey).normalize()
        if (!path.startsWith(dir) || !Files.isRegularFile(path) || Files.size(path) > 20_000_000L) return null
        return runCatching { ImageIO.read(ByteArrayInputStream(Files.readAllBytes(path))) }.getOrNull()
    }

    private fun format(instant: Instant?, zone: ZoneId): String = instant?.let {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone).format(it)
    } ?: "—"
}
