import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { request } from '../../api';
import { Button, Empty, ErrorOrLoading, Header, Panel } from '../../components/ui/primitives';

/** EN: Tenant-scoped, read-only security events. ES: Eventos de seguridad de la empresa, solo lectura. */
type AuditEntry = {
    id: string;
    actorUserId: string | null;
    action: string;
    targetId: string | null;
    createdAt: string;
};

const PAGE_SIZE = 30;

export function SecurityAuditPage() {
    const [page, setPage] = useState(0);
    const [filter, setFilter] = useState('');
    const audit = useQuery({
        queryKey: ['security-audit', page],
        queryFn: () => request<AuditEntry[]>(`/security/audit?page=${page}&size=${PAGE_SIZE}`),
    });
    const rows = (audit.data ?? []).filter(entry =>
        `${entry.action} ${entry.targetId ?? ''} ${entry.actorUserId ?? ''}`.toLowerCase().includes(filter.toLowerCase()),
    );

    return <>
        <Header title="Auditoría de seguridad" subtitle="Registro de acciones de seguridad de esta empresa. Disponible para administradores autorizados." />
        <Panel>
            <label htmlFor="audit-filter">Filtrar registros de esta página</label>
            <input id="audit-filter" className="se-input" value={filter} onChange={event => setFilter(event.target.value)}
                placeholder="Acción, usuario o recurso" />
            <ErrorOrLoading loading={audit.isLoading} error={audit.error}>
                {rows.length === 0 ? <Empty>No hay registros que coincidan en esta página.</Empty> :
                    <div className="table-scroll"><table>
                        <thead><tr><th>Fecha</th><th>Acción</th><th>Usuario</th><th>Recurso</th></tr></thead>
                        <tbody>{rows.map(entry => <tr key={entry.id}>
                            <td>{new Date(entry.createdAt).toLocaleString('es-MX')}</td>
                            <td><strong>{entry.action}</strong></td>
                            <td>{entry.actorUserId ?? 'Sistema'}</td>
                            <td>{entry.targetId ?? '—'}</td>
                        </tr>)}</tbody>
                    </table></div>}
            </ErrorOrLoading>
            <div className="admin-pagination" style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 20 }}>
                <Button variant="secondary" isDisabled={page === 0 || audit.isFetching} onPress={() => setPage(value => Math.max(0, value - 1))}>Anterior</Button>
                <span>Página {page + 1}</span>
                <Button variant="secondary" isDisabled={audit.isFetching || (audit.data?.length ?? 0) < PAGE_SIZE}
                    onPress={() => setPage(value => value + 1)}>Siguiente</Button>
            </div>
        </Panel>
    </>;
}
