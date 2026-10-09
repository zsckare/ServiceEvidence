import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { request, type Customer, type Page, type WorkOrder } from '../../../api';
import { Alert, Header, Panel } from '../../../components/ui/primitives';
import '../../operations/analytics.css';

const states = [
    { value: 'DRAFT', label: 'Borradores' },
    { value: 'SCHEDULED', label: 'Programadas' },
    { value: 'IN_PROGRESS', label: 'En proceso' },
    { value: 'COMPLETED', label: 'Completadas' },
    { value: 'CANCELLED', label: 'Canceladas' },
] as const;

/** EN: Counts use the backend's paginated totalElements, not page length.
 * ES: Los conteos usan totalElements del servidor, no la longitud de una página.
 */
export function Dashboard() {
    const orders = useQuery({
        queryKey: ['dashboard-orders'],
        queryFn: () => request<Page<WorkOrder>>('/work-orders?page=0&size=5'),
    });
    const customers = useQuery({
        queryKey: ['dashboard-customers'],
        queryFn: () => request<Page<Customer>>('/customers?page=0&size=1'),
    });
    const statusQueries = states.map((state) => ({
        ...state,
        // eslint-disable-next-line react-hooks/rules-of-hooks
        result: useQuery({
            queryKey: ['dashboard-status', state.value],
            queryFn: () => request<Page<WorkOrder>>(`/work-orders?page=0&size=1&status=${state.value}`),
        }),
    }));
    const max = Math.max(1, ...statusQueries.map(({ result }) => result.data?.totalElements ?? 0));
    const hasError = Boolean(orders.error || customers.error || statusQueries.some(({ result }) => result.error));

    return <>
        <Header title="Resumen operativo" subtitle="Indicadores actualizados de las órdenes y clientes de tu empresa." />
        {hasError && <Alert>Algunos indicadores no se pudieron cargar. Comprueba tus permisos y la conexión.</Alert>}
        <div className="stats-grid">
            <Panel className="stat"><span>Órdenes registradas</span><strong>{orders.data?.totalElements ?? '—'}</strong><small>Total en el tenant</small></Panel>
            <Panel className="stat"><span>Clientes registrados</span><strong>{customers.data?.totalElements ?? '—'}</strong><small>Total en el tenant</small></Panel>
            <Panel className="stat"><span>Órdenes en proceso</span><strong>{statusQueries[2].result.data?.totalElements ?? '—'}</strong><small>Servicios activos</small></Panel>
            <Panel className="stat"><span>Órdenes completadas</span><strong>{statusQueries[3].result.data?.totalElements ?? '—'}</strong><small>Servicios terminados</small></Panel>
        </div>
        <div className="analytics-layout">
            <Panel>
                <h2>Distribución por estado</h2>
                <p className="analytics-muted">Conteos obtenidos directamente del servidor.</p>
                <div className="analytics-bars">
                    {statusQueries.map(({ value, label, result }) => <div className="analytics-bar-row" key={value}>
                        <div className="analytics-bar-heading"><span>{label}</span><strong>{result.data?.totalElements ?? '—'}</strong></div>
                        <div className="analytics-track"><div className="analytics-fill" style={{ width: `${((result.data?.totalElements ?? 0) / max) * 100}%` }} /></div>
                    </div>)}
                </div>
            </Panel>
            <Panel>
                <h2>Órdenes recientes</h2>
                <p className="analytics-muted">Primeros cinco resultados de la consulta de órdenes.</p>
                <div className="analytics-recent">
                    {orders.data?.content.map(order => <Link to={`/app/orders/${order.id}`} key={order.id} className="analytics-recent-row">
                        <span><strong>{order.title}</strong><small>{order.status} · {order.priority}</small></span>
                        <span>Ver →</span>
                    </Link>)}
                    {orders.data?.content.length === 0 && <p>No hay órdenes registradas.</p>}
                </div>
                <Link to="/app/orders" className="analytics-more">Ver todas las órdenes →</Link>
            </Panel>
        </div>
    </>;
}
