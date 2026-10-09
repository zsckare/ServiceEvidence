import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { request, type Notification } from '../../../api';
import { Alert, Button, Empty, ErrorOrLoading, Header, Panel } from '../../../components/ui/primitives';
import '../analytics.css';

type Filter = 'all' | 'unread' | 'read';

/** EN: Filters notifications locally and keeps mutations server-authoritative.
 * ES: Filtra localmente; marcar como leída siempre actualiza el servidor.
 */
export function Notifications() {
    const [filter, setFilter] = useState<Filter>('all');
    const [error, setError] = useState('');
    const [pending, setPending] = useState<string | null>(null);
    const queryClient = useQueryClient();
    const notifications = useQuery({
        queryKey: ['notifications'],
        queryFn: () => request<Notification[]>('/notifications'),
    });
    const unread = notifications.data?.filter(item => !item.readAt).length ?? 0;
    const visible = notifications.data?.filter(item => filter === 'all' || (filter === 'unread' ? !item.readAt : Boolean(item.readAt))) ?? [];

    async function markRead(id: string) {
        setPending(id);
        setError('');
        try {
            await request(`/notifications/${id}/read`, { method: 'POST' });
            await queryClient.invalidateQueries({ queryKey: ['notifications'] });
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : 'No se pudo marcar la notificación.');
        } finally {
            setPending(null);
        }
    }

    return <>
        <Header title="Notificaciones" subtitle={`${unread} sin leer · Actividad de tu empresa`} />
        <Panel>
            <div className="analytics-filters" role="group" aria-label="Filtrar notificaciones">
                {([['all', 'Todas'], ['unread', 'Sin leer'], ['read', 'Leídas']] as const).map(([value, label]) =>
                    <button type="button" key={value} aria-pressed={filter === value} className={`analytics-filter ${filter === value ? 'analytics-filter--active' : ''}`} onClick={() => setFilter(value)}>{label}</button>,
                )}
            </div>
            {error && <Alert>{error}</Alert>}
            <ErrorOrLoading loading={notifications.isLoading} error={notifications.error}>
                <div className="notification-list">
                    {visible.map(item => <div className="notification-row" key={item.id}>
                        <div className="analytics-notification-body">
                            <strong>{item.title}</strong>
                            <p>{item.message}</p>
                            <small>{new Date(item.createdAt).toLocaleString('es-MX')} · {item.readAt ? 'Leída' : 'Sin leer'}</small>
                        </div>
                        {!item.readAt && <Button variant="secondary" isDisabled={pending !== null} onPress={() => markRead(item.id)}>{pending === item.id ? 'Guardando…' : 'Marcar leída'}</Button>}
                    </div>)}
                    {visible.length === 0 && <Empty>No hay notificaciones para este filtro.</Empty>}
                </div>
            </ErrorOrLoading>
        </Panel>
    </>;
}
