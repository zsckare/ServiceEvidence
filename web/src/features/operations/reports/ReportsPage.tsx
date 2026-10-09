import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { request, type Page, type WorkOrder } from '../../../api';
import { Button, ErrorOrLoading, Header, Panel } from '../../../components/ui/primitives';
import '../analytics.css';

const columns = ['ID', 'Título', 'Estado', 'Prioridad', 'Creada', 'Programada'];

// EN: Escape spreadsheet formulas as well as quotes and CSV separators.
// ES: Neutraliza fórmulas de hojas de cálculo y escapa comillas y separadores.
function csvCell(value: string): string {
    const safe = /^[\s]*[=+@\-\t\r]/.test(value) ? `'${value}` : value;
    return `"${safe.replaceAll('"', '""')}"`;
}

function exportCsv(orders: WorkOrder[]) {
    const rows = orders.map(order => [order.id, order.title, order.status, order.priority, order.createdAt, order.scheduledAt ?? '']);
    const content = '\uFEFF' + [columns, ...rows].map(row => row.map(csvCell).join(',')).join('\r\n');
    const url = URL.createObjectURL(new Blob([content], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `serviceevidence-ordenes-pagina-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** EN: Explicitly exports only the loaded page, not an unbounded tenant dataset.
 * ES: Exporta únicamente la página cargada, no todo el histórico del tenant.
 */
export function ReportsPage() {
    const [page, setPage] = useState(0);
    const orders = useQuery({
        queryKey: ['report-orders', page],
        queryFn: () => request<Page<WorkOrder>>(`/work-orders?page=${page}&size=50`),
    });
    return <>
        <Header title="Reportes operativos" subtitle="Consulta y exporta órdenes para análisis en Excel o cualquier hoja de cálculo." />
        <Panel>
            <h2>Exportación de órdenes</h2>
            <p className="analytics-muted">La exportación incluye únicamente las órdenes de la página visible (máximo 50). No representa el histórico completo.</p>
            <ErrorOrLoading loading={orders.isLoading} error={orders.error}>
                <p><strong>{orders.data?.totalElements ?? 0}</strong> órdenes en total · Página {page + 1} de {Math.max(1, orders.data?.totalPages ?? 1)}</p>
                <div className="analytics-report-actions">
                    <Button variant="secondary" isDisabled={page === 0} onPress={() => setPage(p => p - 1)}>Anterior</Button>
                    <Button variant="secondary" isDisabled={!orders.data || page + 1 >= orders.data.totalPages} onPress={() => setPage(p => p + 1)}>Siguiente</Button>
                    <Button isDisabled={!orders.data?.content.length} onPress={() => exportCsv(orders.data?.content ?? [])}>Exportar página a CSV</Button>
                </div>
                <div className="table-scroll"><table><thead><tr><th>Título</th><th>Estado</th><th>Prioridad</th><th>Fecha</th></tr></thead>
                    <tbody>{orders.data?.content.map(order => <tr key={order.id}><td>{order.title}</td><td>{order.status}</td><td>{order.priority}</td><td>{new Date(order.createdAt).toLocaleDateString('es-MX')}</td></tr>)}</tbody>
                </table></div>
            </ErrorOrLoading>
        </Panel>
    </>;
}
