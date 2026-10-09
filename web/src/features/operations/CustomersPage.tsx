import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { request, type Customer, type CustomerInput, type Page } from '../../api';
import { Button, Empty, ErrorOrLoading, Header, Panel, Status } from '../../components/ui/primitives';
import { FormActions, FormField, OperationError } from './shared';

const blank: CustomerInput = {
    name: '', legalName: null, taxId: null, email: null, phone: null, notes: null, active: true,
};

export function CustomersPage() {
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [editing, setEditing] = useState<Customer | 'new' | null>(null);
    const [form, setForm] = useState<CustomerInput>(blank);
    const queryClient = useQueryClient();
    const customers = useQuery({
        queryKey: ['customers', search, page],
        queryFn: () => request<Page<Customer>>(
            `/customers?page=${page}&size=20&search=${encodeURIComponent(search)}`,
        ),
    });
    const save = useMutation({
        mutationFn: () => request<Customer>(
            editing === 'new' ? '/customers' : `/customers/${(editing as Customer).id}`,
            { method: editing === 'new' ? 'POST' : 'PUT', body: JSON.stringify(form) },
        ),
        onSuccess: async () => {
            setEditing(null);
            await queryClient.invalidateQueries({ queryKey: ['customers'] });
            await queryClient.invalidateQueries({ queryKey: ['dashboard-customers'] });
        },
    });

    async function begin(customer?: Customer) {
        setEditing(customer ?? 'new');
        const fullCustomer = customer ? await request<CustomerInput>(`/customers/${customer.id}`) : null;
        setForm(fullCustomer
            ? { ...blank, ...fullCustomer }
            : { ...blank });
        save.reset();
    }

    function update(key: keyof CustomerInput, value: string | boolean) {
        setForm((previous) => ({ ...previous, [key]: value || (key === 'name' ? '' : null) }));
    }

    function submit(event: FormEvent) {
        event.preventDefault();
        save.mutate();
    }

    return (
        <>
            <Header
                title="Clientes"
                subtitle="Gestiona los clientes y sus ubicaciones."
                actions={<Button onPress={() => begin()}>Nuevo cliente</Button>}
            />
            {editing && (
                <Panel>
                    <h2>{editing === 'new' ? 'Registrar cliente' : 'Editar cliente'}</h2>
                    <form className="operation-form" onSubmit={submit}>
                        <FormField label="Nombre comercial" required value={form.name} onChange={(v) => update('name', v)} />
                        <FormField label="Correo" type="email" value={form.email ?? ''} onChange={(v) => update('email', v)} />
                        <FormField label="Teléfono" value={form.phone ?? ''} onChange={(v) => update('phone', v)} />
                        <label className="operation-checkbox">
                            <input type="checkbox" checked={form.active} onChange={(e) => update('active', e.target.checked)} />
                            Cliente activo
                        </label>
                        <OperationError error={save.error} />
                        <FormActions busy={save.isPending} onCancel={() => setEditing(null)} />
                    </form>
                </Panel>
            )}
            <Panel>
                <div className="operation-toolbar">
                    <input
                        className="se-input"
                        aria-label="Buscar clientes"
                        placeholder="Buscar clientes…"
                        value={search}
                        onChange={(event) => { setSearch(event.target.value); setPage(0); }}
                    />
                </div>
                <ErrorOrLoading loading={customers.isLoading} error={customers.error}>
                    {customers.data?.content.length === 0 ? <Empty>No se encontraron clientes.</Empty> : (
                        <div className="table-scroll">
                            <table>
                                <thead><tr><th>Cliente</th><th>Correo</th><th>Teléfono</th><th>Estado</th><th>Acciones</th></tr></thead>
                                <tbody>
                                    {customers.data?.content.map((customer) => (
                                        <tr key={customer.id}>
                                            <td><strong>{customer.name}</strong></td>
                                            <td>{customer.email || '—'}</td>
                                            <td>{customer.phone || '—'}</td>
                                            <td><Status active={customer.active} label={customer.active ? 'Activo' : 'Inactivo'} /></td>
                                            <td className="operation-row-actions">
                                                <Link to={`/app/customers/${customer.id}/locations`}>Ubicaciones</Link>
                                                <Button variant="secondary" onPress={() => begin(customer)}>Editar</Button>
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
                    <span>Página {page + 1} de {Math.max(1, customers.data?.totalPages ?? 1)}</span>
                    <Button variant="secondary" isDisabled={!customers.data || page + 1 >= customers.data.totalPages} onPress={() => setPage((n) => n + 1)}>Siguiente</Button>
                </div>
            </Panel>
        </>
    );
}
