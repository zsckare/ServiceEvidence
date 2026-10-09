package com.serviceevidence.backend.reports

import com.serviceevidence.backend.evidence.ServiceEvidence
import com.serviceevidence.backend.workorders.WorkOrder
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * EN: Compiles the JRXML once and fills it with tenant-scoped work order data.
 * ES: Compila el JRXML una vez y lo rellena con datos de la orden filtrados por empresa.
 */
@Service
class ServiceReportJasperService {
    private val template: JasperReport by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val resource = javaClass.getResourceAsStream("/reports/service_report.jrxml")
            ?: error("Missing Jasper template: /reports/service_report.jrxml")
        resource.use { JasperCompileManager.compileReport(it) }
    }

    /** EN: Generates PDF bytes. ES: Genera los bytes del documento PDF. */
    fun generate(order: WorkOrder, customerName: String, locationName: String, evidence: List<ServiceEvidence>): ByteArray {
        val parameters: Map<String, Any> = mapOf(
            "ORDER_ID" to order.id.toString(),
            "TITLE" to order.title,
            "DESCRIPTION" to (order.description ?: "—"),
            "CUSTOMER" to customerName,
            "LOCATION" to locationName,
            "STATUS" to order.status,
            "PRIORITY" to order.priority,
            "CREATED" to format(order.createdAt),
            "SCHEDULED" to format(order.scheduledAt),
            "STARTED" to format(order.startedAt),
            "COMPLETED" to format(order.completedAt),
            "EVIDENCE_COUNT" to evidence.size.toString()
        )
        val rows = evidence.map { item ->
            EvidenceRow(item.kind, item.id.toString(), item.notes ?: "", format(item.createdAt))
        }.ifEmpty { listOf(EvidenceRow("—", "No evidence registered", "", "")) }
        val filled = JasperFillManager.fillReport(template, parameters, JRBeanCollectionDataSource(rows))
        return JasperExportManager.exportReportToPdf(filled)
    }

    private fun format(instant: Instant?): String = instant?.let {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC).format(it)
    } ?: "—"

    /** EN: JasperBean row for the evidence table. ES: Fila para la tabla de evidencias. */
    data class EvidenceRow(val kind: String, val id: String, val notes: String, val createdAt: String)
}
