import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { request } from '../../api';
import { Alert, Button, Empty, ErrorOrLoading, Header, Panel, Status } from '../../components/ui/primitives';

type Role = 'TENANT_ADMIN' | 'SUPERVISOR' | 'TECHNICIAN' | 'CLIENT';
type Member = { id: string; email: string; fullName: string; role: Role; enabled: boolean };
type NewMember = { email: string; fullName: string; password: string; role: Role };

const initial: NewMember = { email: '', fullName: '', password: '', role: 'TECHNICIAN' };

/** EN: Tenant user administration. Server enforces role authorization.
 *  ES: Administración de usuarios. El servidor valida los permisos.
 */
export function UsersPage() {
    const queryClient = useQueryClient();
    const [form, setForm] = useState<NewMember>(initial);
    const [creating, setCreating] = useState(false);
    const members = useQuery({ queryKey: ['tenant-members'], queryFn: () => request<Member[]>('/users') });
    const create = useMutation({
        mutationFn: () => request<Member>('/users', { method: 'POST', body: JSON.stringify(form) }),
        onSuccess: async () => {
            setCreating(false);
            setForm(initial);
            await queryClient.invalidateQueries({ queryKey: ['tenant-members'] });
        },
    });

    const update = useMutation({
        mutationFn: ({ id, changes }: { id: string; changes: Partial<Pick<Member, 'role' | 'enabled'>> }) =>
            request<Member>(`/users/${id}`, { method: 'PATCH', body: JSON.stringify(changes) }),
        onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['tenant-members'] }); },
    });

    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        create.mutate();
    }

    return (
        <>
            <Header title="Usuarios y roles" subtitle="Gestiona el equipo autorizado de esta empresa."
                actions={<Button onPress={() => setCreating((value) => !value)}>{creating ? 'Cerrar' : 'Nuevo usuario'}</Button>} />
            {creating && (
                <Panel>
                    <h2>Registrar usuario</h2>
                    <form className="admin-form" onSubmit={submit}>
                        <label>Nombre completo
                            <input required className="se-input" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
                        </label>
                        <label>Correo electrónico
                            <input required type="email" className="se-input" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
                        </label>
                        <label>Contraseña inicial (mínimo 12 caracteres)
                            <input required type="password" minLength={12} autoComplete="new-password" className="se-input" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
                        </label>
                        <label>Rol
                            <select className="se-input" value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as Role })}>
                                <option value="TECHNICIAN">Técnico</option>
                                <option value="SUPERVISOR">Supervisor</option>
                                <option value="TENANT_ADMIN">Administrador de empresa</option>
                                <option value="CLIENT">Cliente</option>
                            </select>
                        </label>
                        {create.error && <Alert>{create.error.message}</Alert>}
                        <Button type="submit" isDisabled={create.isPending}>{create.isPending ? 'Guardando…' : 'Crear usuario'}</Button>
                    </form>
                </Panel>
            )}
            {update.error && <Alert>{update.error.message}</Alert>}
            <Panel>
                <ErrorOrLoading loading={members.isLoading} error={members.error}>
                    {members.data?.length === 0 ? <Empty>Esta empresa todavía no tiene usuarios.</Empty> : (
                        <div className="table-scroll">
                            <table>
                                <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead>
                                <tbody>{members.data?.map((member) => (
                                    <tr key={member.id}>
                                        <td><strong>{member.fullName}</strong></td>
                                        <td>{member.email}</td>
                                        <td><select aria-label={`Rol de ${member.fullName}`} className="se-input"
                                            disabled={update.isPending}
                                            value={member.role}
                                            onChange={(event) => update.mutate({ id: member.id, changes: { role: event.target.value as Role } })}>
                                            <option value="TECHNICIAN">Técnico</option>
                                            <option value="SUPERVISOR">Supervisor</option>
                                            <option value="TENANT_ADMIN">Administrador</option>
                                            <option value="CLIENT">Cliente</option>
                                        </select></td>
                                        <td><Status active={member.enabled} label={member.enabled ? 'Activo' : 'Inactivo'} /></td>
                                        <td><Button variant="secondary" isDisabled={update.isPending}
                                            onPress={() => {
                                                if (window.confirm(`¿${member.enabled ? 'Desactivar' : 'Activar'} a ${member.fullName}?`)) {
                                                    update.mutate({ id: member.id, changes: { enabled: !member.enabled } });
                                                }
                                            }}>{member.enabled ? 'Desactivar' : 'Activar'}</Button></td>
                                    </tr>
                                ))}</tbody>
                            </table>
                        </div>
                    )}
                </ErrorOrLoading>
            </Panel>
        </>
    );
}
