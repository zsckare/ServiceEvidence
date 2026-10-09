import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    request,
    type TenantMember,
    type WorkOrderAssignment,
} from '../../api';
import { Button, Empty, ErrorOrLoading, Panel } from '../../components/ui/primitives';
import { OperationError } from './shared';

/** EN: Assignment mutations are restricted server-side to admin/supervisor.
 * ES: El backend valida los permisos para asignar o retirar técnicos.
 */
export function AssignmentsPanel({ orderId, closed }: { orderId: string; closed: boolean }) {
    const queryClient = useQueryClient();
    const [technicianId, setTechnicianId] = useState('');
    const [primary, setPrimary] = useState(false);

    const members = useQuery({
        queryKey: ['tenant-members'],
        queryFn: () => request<TenantMember[]>('/users'),
    });
    const assignments = useQuery({
        queryKey: ['assignments', orderId],
        queryFn: () => request<WorkOrderAssignment[]>(`/work-orders/${orderId}/assignments`),
    });
    const add = useMutation({
        mutationFn: () => request<WorkOrderAssignment>(`/work-orders/${orderId}/assignments`, {
            method: 'POST',
            body: JSON.stringify({ technicianId, primary }),
        }),
        onSuccess: async () => {
            setTechnicianId('');
            setPrimary(false);
            await queryClient.invalidateQueries({ queryKey: ['assignments', orderId] });
        },
    });
    const remove = useMutation({
        mutationFn: (id: string) => request<void>(`/work-orders/${orderId}/assignments/${id}`, {
            method: 'DELETE',
        }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['assignments', orderId] }),
    });

    const technicians = (members.data ?? []).filter((member) => member.role === 'TECHNICIAN' && member.enabled);
    const assigned = new Set((assignments.data ?? []).map((assignment) => assignment.technicianId));

    function submit(event: FormEvent) {
        event.preventDefault();
        if (technicianId) add.mutate();
    }

    return (
        <Panel>
            <h2>Técnicos asignados</h2>
            <ErrorOrLoading loading={assignments.isLoading} error={assignments.error}>
                {assignments.data?.length === 0 ? <Empty>No hay técnicos asignados.</Empty> : (
                    <div className="assignment-list">
                        {assignments.data?.map((assignment) => {
                            const member = members.data?.find((candidate) => candidate.id === assignment.technicianId);
                            return (
                                <div className="assignment-item" key={assignment.id}>
                                    <div>
                                        <strong>{member?.fullName ?? assignment.technicianId}</strong>
                                        <small>{member?.email ?? 'Técnico asignado'} {assignment.primary ? '· Principal' : ''}</small>
                                    </div>
                                    {!closed && (
                                        <Button variant="secondary" isDisabled={remove.isPending} onPress={() => remove.mutate(assignment.technicianId)}>
                                            Retirar
                                        </Button>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </ErrorOrLoading>
            {!closed && (
                <form className="assignment-form" onSubmit={submit}>
                    <label className="operation-field">
                        <span>Agregar técnico</span>
                        <select className="se-input" required value={technicianId} onChange={(event) => setTechnicianId(event.target.value)}>
                            <option value="">Seleccionar técnico</option>
                            {technicians.filter((member) => !assigned.has(member.id)).map((member) => (
                                <option key={member.id} value={member.id}>{member.fullName} · {member.email}</option>
                            ))}
                        </select>
                    </label>
                    <label className="operation-checkbox">
                        <input type="checkbox" checked={primary} onChange={(event) => setPrimary(event.target.checked)} />
                        Responsable principal
                    </label>
                    <Button type="submit" isDisabled={!technicianId || add.isPending}>Asignar técnico</Button>
                </form>
            )}
            <OperationError error={members.error || add.error || remove.error} />
        </Panel>
    );
}
