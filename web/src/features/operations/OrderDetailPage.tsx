import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    downloadPrivateFile, request,
    type Evidence, type WorkOrder, type WorkOrderEvent,
} from '../../api';
import { Alert, Button, Empty, ErrorOrLoading, Header, Panel } from '../../components/ui/primitives';
import { OperationError } from './shared';

export function OrderDetailPage() {
    const { orderId = '' } = useParams();
    const queryClient = useQueryClient();
    const [status, setStatus] = useState('');
    const [file, setFile] = useState<File | null>(null);
    const [kind, setKind] = useState('PHOTO');
    const [notes, setNotes] = useState('');
    const [actionError, setActionError] = useState<string | null>(null);

    const order = useQuery({
        queryKey: ['order', orderId],
        queryFn: () => request<WorkOrder>(`/work-orders/${orderId}`),
    });
    const evidence = useQuery({
        queryKey: ['evidence', orderId],
        queryFn: () => request<Evidence[]>(`/work-orders/${orderId}/evidence`),
    });
    const events = useQuery({
        queryKey: ['order-events', orderId],
        queryFn: () => request<WorkOrderEvent[]>(`/work-orders/${orderId}/events`),
    });

    const updateStatus = useMutation({
        mutationFn: () => request<WorkOrder>(`/work-orders/${orderId}/status`, {
            method: 'POST',
            body: JSON.stringify({ status }),
        }),
        onSuccess: async () => {
            setStatus('');
            await queryClient.invalidateQueries({ queryKey: ['order', orderId] });
            await queryClient.invalidateQueries({ queryKey: ['order-events', orderId] });
            await queryClient.invalidateQueries({ queryKey: ['orders'] });
        },
    });

    const upload = useMutation({
        mutationFn: async () => {
            if (!file) throw new Error('Selecciona un archivo.');
            const form = new FormData();
            form.append('file', file);
            form.append('kind', kind);
            if (notes.trim()) form.append('notes', notes.trim());
            return request<Evidence>(`/work-orders/${orderId}/files`, {
                method: 'POST',
                body: form,
            });
        },
        onSuccess: async () => {
            setFile(null);
            setNotes('');
            await queryClient.invalidateQueries({ queryKey: ['evidence', orderId] });
        },
    });

    async function download(path: string, filename: string) {
        setActionError(null);
        try {
            await downloadPrivateFile(path, filename);
        } catch (error) {
            setActionError(error instanceof Error ? error.message : 'No se pudo descargar.');
        }
    }

    function submitUpload(event: FormEvent) {
        event.preventDefault();
        upload.mutate();
    }

    return (
        <>
            <Link to="/app/orders" className="operation-back">← Volver a órdenes</Link>
            <Header
                title={order.data?.title ?? 'Detalle de orden'}
                subtitle="Seguimiento, evidencias y reporte de servicio."
                actions={
                    <Button
                        variant="secondary"
                        onPress={() => download(`/work-orders/${orderId}/report.pdf`, `reporte-${orderId}.pdf`)}
                    >
                        Descargar reporte PDF
                    </Button>
                }
            />
            {actionError && <Alert>{actionError}</Alert>}
            <ErrorOrLoading loading={order.isLoading} error={order.error}>
                {order.data && (
                    <Panel>
                        <div className="operation-details">
                            <div><span>Estado</span><strong>{order.data.status}</strong></div>
                            <div><span>Prioridad</span><strong>{order.data.priority}</strong></div>
                            <div><span>Creación</span><strong>{new Date(order.data.createdAt).toLocaleString('es-MX')}</strong></div>
                        </div>
                        <form className="operation-inline-form" onSubmit={(event) => { event.preventDefault(); updateStatus.mutate(); }}>
                            <label className="operation-field">
                                <span>Cambiar estado</span>
                                <select className="se-input" required value={status} onChange={(e) => setStatus(e.target.value)}>
                                    <option value="">Seleccionar estado</option>
                                    {['DRAFT', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map((item) => (
                                        <option key={item} value={item}>{item}</option>
                                    ))}
                                </select>
                            </label>
                            <Button type="submit" isDisabled={!status || updateStatus.isPending}>Actualizar</Button>
                        </form>
                        <OperationError error={updateStatus.error} />
                    </Panel>
                )}
            </ErrorOrLoading>
            <Panel>
                <h2>Evidencias</h2>
                <ErrorOrLoading loading={evidence.isLoading} error={evidence.error}>
                    {evidence.data?.length === 0 ? <Empty>Aún no hay evidencias.</Empty> : (
                        <div className="operation-evidence-grid">
                            {evidence.data?.map((item) => (
                                <div className="operation-evidence" key={item.id}>
                                    <div className="operation-evidence-icon">{item.kind === 'SIGNATURE' ? '✍' : '▧'}</div>
                                    <strong>{item.kind}</strong>
                                    <span>{item.notes || new Date(item.createdAt).toLocaleString('es-MX')}</span>
                                    <Button
                                        variant="secondary"
                                        onPress={() => download(
                                            `/work-orders/${orderId}/files/${item.id}`,
                                            `evidencia-${item.id}`,
                                        )}
                                    >
                                        Descargar
                                    </Button>
                                </div>
                            ))}
                        </div>
                    )}
                </ErrorOrLoading>
                {order.data && !['COMPLETED', 'CANCELLED'].includes(order.data.status) && (
                    <form className="operation-upload" onSubmit={submitUpload}>
                        <h3>Agregar evidencia</h3>
                        <label className="operation-field">
                            <span>Tipo</span>
                            <select className="se-input" value={kind} onChange={(e) => setKind(e.target.value)}>
                                <option value="PHOTO">Fotografía</option>
                                <option value="SIGNATURE">Firma</option>
                                <option value="DOCUMENT">Documento</option>
                            </select>
                        </label>
                        <label className="operation-field">
                            <span>Archivo</span>
                            <input
                                type="file"
                                required
                                accept="image/png,image/jpeg,image/webp,application/pdf"
                                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                            />
                        </label>
                        <label className="operation-field">
                            <span>Notas</span>
                            <input className="se-input" value={notes} onChange={(event) => setNotes(event.target.value)} />
                        </label>
                        <Button type="submit" isDisabled={!file || upload.isPending}>
                            {upload.isPending ? 'Subiendo…' : 'Subir evidencia'}
                        </Button>
                        <OperationError error={upload.error} />
                    </form>
                )}
            </Panel>
            <Panel>
                <h2>Historial de actividad</h2>
                <ErrorOrLoading loading={events.isLoading} error={events.error}>
                    {events.data?.length === 0 ? <Empty>Sin movimientos registrados.</Empty> : (
                        <div className="operation-event-list">
                            {events.data?.map((event, index) => (
                                <div key={`${event.createdAt}-${index}`}>
                                    <strong>{event.action}</strong>
                                    <span>{event.details || '—'}</span>
                                    <small>{new Date(event.createdAt).toLocaleString('es-MX')}</small>
                                </div>
                            ))}
                        </div>
                    )}
                </ErrorOrLoading>
            </Panel>
        </>
    );
}
