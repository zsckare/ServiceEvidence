import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { request, type Customer, type Location, type LocationInput, type Page } from '../../api';
import { Button, Empty, ErrorOrLoading, Header, Panel, Status } from '../../components/ui/primitives';
import { FormActions, FormField, OperationError } from './shared';

const emptyLocation: LocationInput = {
    name: '', addressLine1: '', addressLine2: null, city: '', state: null,
    postalCode: null, country: 'MX', contactName: null, contactPhone: null,
    notes: null, active: true,
};

export function LocationsPage() {
    const { customerId = '' } = useParams();
    const [editing, setEditing] = useState<Location | 'new' | null>(null);
    const [form, setForm] = useState<LocationInput>(emptyLocation);
    const queryClient = useQueryClient();
    const customer = useQuery({
        queryKey: ['customer', customerId],
        queryFn: () => request<Customer>(`/customers/${customerId}`),
    });
    const locations = useQuery({
        queryKey: ['locations', customerId],
        queryFn: () => request<Page<Location>>(`/customers/${customerId}/locations?page=0&size=100`),
    });
    const save = useMutation({
        mutationFn: () => request<Location>(
            editing === 'new'
                ? `/customers/${customerId}/locations`
                : `/customers/${customerId}/locations/${(editing as Location).id}`,
            { method: editing === 'new' ? 'POST' : 'PUT', body: JSON.stringify(form) },
        ),
        onSuccess: async () => {
            setEditing(null);
            await queryClient.invalidateQueries({ queryKey: ['locations', customerId] });
        },
    });

    function begin(location?: Location) {
        setEditing(location ?? 'new');
        setForm(location ? { ...location } : { ...emptyLocation });
        save.reset();
    }

    function set(key: keyof LocationInput, value: string | boolean) {
        setForm((previous) => ({ ...previous, [key]: value }));
    }

    function submit(event: FormEvent) {
        event.preventDefault();
        save.mutate();
    }

    return (
        <>
            <Link to="/app/customers" className="operation-back">← Volver a clientes</Link>
            <Header
                title={`Ubicaciones · ${customer.data?.name ?? 'Cliente'}`}
                subtitle="Instalaciones y direcciones donde se realizan los servicios."
                actions={<Button onPress={() => begin()}>Nueva ubicación</Button>}
            />
            {editing && (
                <Panel>
                    <h2>{editing === 'new' ? 'Registrar ubicación' : 'Editar ubicación'}</h2>
                    <form className="operation-form" onSubmit={submit}>
                        <FormField label="Nombre" required value={form.name} onChange={(v) => set('name', v)} />
                        <FormField label="Dirección" required value={form.addressLine1} onChange={(v) => set('addressLine1', v)} />
                        <FormField label="Ciudad" required value={form.city} onChange={(v) => set('city', v)} />
                        <FormField label="Estado" value={form.state ?? ''} onChange={(v) => set('state', v)} />
                        <FormField label="Código postal" value={form.postalCode ?? ''} onChange={(v) => set('postalCode', v)} />
                        <FormField label="País" required value={form.country} onChange={(v) => set('country', v)} />
                        <FormField label="Contacto" value={form.contactName ?? ''} onChange={(v) => set('contactName', v)} />
                        <FormField label="Teléfono de contacto" value={form.contactPhone ?? ''} onChange={(v) => set('contactPhone', v)} />
                        <label className="operation-checkbox">
                            <input type="checkbox" checked={form.active} onChange={(e) => set('active', e.target.checked)} />
                            Ubicación activa
                        </label>
                        <OperationError error={save.error} />
                        <FormActions busy={save.isPending} onCancel={() => setEditing(null)} />
                    </form>
                </Panel>
            )}
            <Panel>
                <ErrorOrLoading loading={locations.isLoading} error={locations.error}>
                    {locations.data?.content.length === 0 ? <Empty>No hay ubicaciones registradas.</Empty> : (
                        <div className="table-scroll">
                            <table>
                                <thead><tr><th>Ubicación</th><th>Dirección</th><th>Estado</th><th>Acciones</th></tr></thead>
                                <tbody>
                                    {locations.data?.content.map((location) => (
                                        <tr key={location.id}>
                                            <td><strong>{location.name}</strong></td>
                                            <td>{location.addressLine1}, {location.city}</td>
                                            <td><Status active={location.active} label={location.active ? 'Activa' : 'Inactiva'} /></td>
                                            <td><Button variant="secondary" onPress={() => begin(location)}>Editar</Button></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </ErrorOrLoading>
            </Panel>
        </>
    );
}
