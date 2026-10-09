import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { request } from '../../api';
import { Alert, Button, ErrorOrLoading, Header, Panel } from '../../components/ui/primitives';

type TenantSettings = { displayName: string; supportEmail: string; timezone: string };

/** EN: Per-tenant settings, loaded only after tenant context is selected.
 *  ES: Preferencias de empresa obtenidas dentro del contexto del tenant.
 */
export function SettingsPage() {
    const queryClient = useQueryClient();
    const settings = useQuery({ queryKey: ['tenant-settings'], queryFn: () => request<TenantSettings>('/settings') });
    const [form, setForm] = useState<TenantSettings>({ displayName: '', supportEmail: '', timezone: 'UTC' });
    useEffect(() => { if (settings.data) setForm(settings.data); }, [settings.data]);
    const save = useMutation({
        mutationFn: () => request<TenantSettings>('/settings', { method: 'PUT', body: JSON.stringify(form) }),
        onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['tenant-settings'] }); },
    });
    function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); save.mutate(); }

    return (
        <>
            <Header title="Configuración de empresa" subtitle="Personaliza la identidad y preferencias de tu organización." />
            <Panel>
                <ErrorOrLoading loading={settings.isLoading} error={settings.error}>
                    <form className="admin-form" onSubmit={submit}>
                        <label>Nombre visible
                            <input className="se-input" maxLength={160} value={form.displayName} onChange={(event) => setForm({ ...form, displayName: event.target.value })} />
                        </label>
                        <label>Correo de soporte
                            <input className="se-input" type="email" maxLength={200} value={form.supportEmail} onChange={(event) => setForm({ ...form, supportEmail: event.target.value })} />
                        </label>
                        <label>Zona horaria (identificador IANA)
                            <input className="se-input" required maxLength={80} placeholder="America/Mexico_City" value={form.timezone} onChange={(event) => setForm({ ...form, timezone: event.target.value })} />
                        </label>
                        {save.error && <Alert>{save.error.message}</Alert>}
                        {save.isSuccess && <p role="status">Configuración guardada.</p>}
                        <Button type="submit" isDisabled={save.isPending}>{save.isPending ? 'Guardando…' : 'Guardar configuración'}</Button>
                    </form>
                </ErrorOrLoading>
            </Panel>
        </>
    );
}
