import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { request, type Customer, type Location, type Page, type WorkOrder, type WorkOrderInput } from '../../api';
import { Button, Empty, ErrorOrLoading, Header, Panel } from '../../components/ui/primitives';
import { FormActions, FormField, OperationError } from './shared';

const emptyOrder: WorkOrderInput = {
    title: '', customerId: '', locationId: '', description: null,
    priority: 'MEDIUM', scheduledAt: null,
};

export function OrdersPage() {
    const [page, setPage] = useState(0);
    const [editing, setEditing] = useState<WorkOrder | 'new' | null>(null);
    const [form, setForm] = useState<WorkOrderInput>(emptyOrder);
    const queryClient = useQueryClient();
    const orders = useQuery({
        queryKey: ['orders', page],
        queryFn: () => request<Page<WorkOrder>>(`/work-orders?page=${page}&size=20`),
    });
    const customers = useQuery({
        queryKey: ['order-form-customers'],
        queryFn: () => request<Page<Customer>>('/customers?page=0&size=100'),
        enabled: Boolean(editing),
    });
    const locations = useQuery({
        queryKey: ['order-form-locations', form.customerId],
        queryFn: () => request<Page<Location>>(`/customers/${form.customerId}/locations?page=0&size=100`),
        enabled: Boolean(editing && form.customerId),
    });
    const save = useMutation({
        mutationFn: () => request<WorkOrder>(
            editing === 'new' ? '/work-orders' : `/work-orders/${(editing as WorkOrder).id}`,
            { method: editing === 'new' ? 'POST' : 'PUT', body: JSON.stringify(form) },
        ),
        onSuccess: async () => {
            setEditing(null);
            await queryClient.invalidateQueries({ queryKey: ['orders'] });
            await queryClient.invalidateQueries({ queryKey: ['dashboard-orders'] });
        },
    });

    async function begin(order?: WorkOrder) {
        setEditing(order ?? 'new');
        const fullOrder = order ? await request<WorkOrder>(`/work-orders/${order.id}`) : null;
        setForm(fullOrder
            ? {
                title: fullOrder!.title, customerId: fullOrder!.customerId,
                locationId: fullOrder!.locationId, priority: fullOrder!.priority,
                description: (fullOrder as WorkOrder & { description?: string | null }).description ?? null,
                scheduledAt: fullOrder!.scheduledAt,
            }
            : { ...emptyOrder });
        save.reset();
    }

    function set(key: keyof WorkOrderInput, value: string | null) {
        setForm((previous) => ({
            ...previous,
            [key]: value,
            ...(key === 'customerId' ? { locationId: '' } : {}),
        }));
    }

    function submit(event: FormEvent) {
        event.preventDefault();
        save.mutate();
    }

    return (
        <>
            <Header
                title="Órdenes de trabajo"
                subtitle="Planifica y consulta las actividades de campo."
                actions={<Button onPress={() => begin()}>Nueva orden</Button>}
            />
            {editing && (
                <Panel>
                    <h2>{editing === 'new' ? 'Crear orden' : 'Editar orden'}</h2>
                    <form className="operation-form" onSubmit={submit}>
                        <FormField label="Título" required value={form.title} onChange={(v) => set('title', v)} />
                        <FormField label="Cliente">
                            <select className="se-input" required value={form.customerId} onChange={(e) => set('customerId', e.target.value)}>
                                <option value="">Seleccionar cliente</option>
                                {customers.data?.content.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                            </select>
                        </FormField>
                        <FormField label="Ubicación">
                            <select className="se-input" required value={form.locationId} onChange={(e) => set('locationId', e.target.value)}>
                                <option value="">Seleccionar ubicación</option>
                                {locations.data?.content.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                            </select>
                        </FormField>
                        <FormField label="Prioridad">
                            <select className="se-input" value={form.priority} onChange={(e) => set('priority', e.target.value)}>
                                <option value="LOW">Baja</option>
                                <option value="MEDIUM">Media</option>
                                <option value="HIGH">Alta</option>
                                <option value="URGENT">Urgente</option>
                            </select>
                        </FormField>
                        <FormField label="Descripción">
                            <textarea className="se-input" rows={3} value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} />
                        </FormField>
                        <OperationError error={save.error} />
                        <FormActions busy={save.isPending} onCancel={() => setEditing(null)} />
                    </form>
                </Panel>
            )}
            <Panel>
                <ErrorOrLoading loading={orders.isLoading} error={orders.error}>
                    {orders.data?.content.length === 0 ? <Empty>No hay órdenes registradas.</Empty> : (
                        <div className="table-scroll">
                            <table>
                                <thead><tr><th>Orden</th><th>Estado</th><th>Prioridad</th><th>Creada</th><th>Acciones</th></tr></thead>
                                <tbody>
                                    {orders.data?.content.map((order) => (
                                        <tr key={order.id}>
                                            <td><strong>{order.title}</strong></td>
                                            <td><span className="pill">{order.status}</span></td>
                                            <td>{order.priority}</td>
                                            <td>{new Date(order.createdAt).toLocaleDateString('es-MX')}</td>
                                            <td className="operation-row-actions">
                                                <Link to={`/app/orders/${order.id}`}>Ver detalle</Link>
                                                <Button variant="secondary" onPress={() => begin(order)}>Editar</Button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </ErrorOrLoading>
                <div className="pagination">
                    <Button variant="secondary" isDisabled={page === 0} onPress={() => setPage((n) => n - 1)}>Anterior</Button>
                    <span>Página {page + 1} de {Math.max(1, orders.data?.totalPages ?? 1)}</span>
                    <Button variant="secondary" isDisabled={!orders.data || page + 1 >= orders.data.totalPages} onPress={() => setPage((n) => n + 1)}>Siguiente</Button>
                </div>
            </Panel>
        </>
    );
}
