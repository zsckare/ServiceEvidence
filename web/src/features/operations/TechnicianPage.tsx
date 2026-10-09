import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { request, type WorkOrder } from '../../api';
import { Empty, ErrorOrLoading, Header, Panel } from '../../components/ui/primitives';

/** EN: Technician's server-scoped assignments; no client-side authorization shortcuts.
 * ES: Órdenes asignadas filtradas por el servidor, sin confiar en filtros locales.
 */
export function TechnicianPage() {
    const orders = useQuery({
        queryKey: ['technician-orders'],
        queryFn: () => request<WorkOrder[]>('/work-orders/mine'),
    });

    return <>
        <Header title="Mis servicios" subtitle="Órdenes asignadas a tu cuenta." />
        <Panel>
            <ErrorOrLoading loading={orders.isLoading} error={orders.error}>
                {orders.data?.length === 0 ? <Empty>No tienes servicios asignados.</Empty> : (
                    <div className="technician-grid">
                        {orders.data?.map((order) => <Link className="technician-card" key={order.id} to={`/app/orders/${order.id}`}>
                            <span className="pill">{order.status}</span>
                            <h3>{order.title}</h3>
                            <p>Prioridad: {order.priority}</p>
                            <small>{order.scheduledAt ? `Programada: ${new Date(order.scheduledAt).toLocaleString('es-MX')}` : 'Sin fecha programada'}</small>
                            <strong>Ver servicio →</strong>
                        </Link>)}
                    </div>
                )}
            </ErrorOrLoading>
        </Panel>
    </>;
}
