import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell01, CheckCircle, ClipboardCheck, SearchLg } from '@untitledui/icons';
import { request, type Customer, type Notification, type Page, type WorkOrder } from '../../api';
import { Alert, Button, Empty, ErrorOrLoading, Header, Panel, Status } from '../../components/ui/primitives';

export function Orders() { const [page, setPage] = useState(0);
  const data = useQuery({ queryKey: ['orders', page], queryFn: () => request<Page<WorkOrder>>(`/work-orders?page=${page}&size=20`) });
  return <>
      <Header title="Órdenes de trabajo" subtitle="Consulta y supervisa las órdenes de tu empresa." />
      <Panel>
      <ErrorOrLoading loading={data.isLoading} error={data.error}>
      <div className="table-scroll">
      <table>
      <thead>
      <tr>
      <th>Título</th>
      <th>Estado</th>
      <th>Prioridad</th>
      <th>Creada</th>
      </tr>
      </thead>
      <tbody>{data.data?.content.map(o => <tr key={o.id}>
      <td>
      <strong>{o.title}</strong>
      </td>
      <td>
      <span className="pill">{o.status}</span>
      </td>
      <td>{o.priority}</td>
      <td>{new Date(o.createdAt).toLocaleDateString('es-MX')}</td>
      </tr>)}</tbody>
      </table>
      </div>
      <div className="pagination">
      <Button variant="secondary" isDisabled={page === 0} onPress={() => setPage(v => v - 1)}>Anterior</Button>
      <span>Página {page + 1} de {Math.max(1, data.data?.totalPages ?? 1)}</span>
      <Button variant="secondary" isDisabled={!data.data || page + 1 >= data.data.totalPages} onPress={() => setPage(v => v + 1)}>Siguiente</Button>
      </div>
      </ErrorOrLoading>
      </Panel>
      </>; }

export function Customers() { const [search, setSearch] = useState('');
  const data = useQuery({ queryKey: ['customers', search], queryFn: () => request<Page<Customer>>(`/customers?page=0&size=100&search=${encodeURIComponent(search)}`) });
  return <>
      <Header title="Clientes" subtitle="Directorio de clientes de tu empresa." />
      <Panel>
      <div className="search-box search-box--limited">
      <SearchLg size={18} />
      <input aria-label="Buscar clientes" placeholder="Buscar clientes…" value={search} onChange={e => setSearch(e.target.value)} />
      </div>
      <ErrorOrLoading loading={data.isLoading} error={data.error}>
      <div className="table-scroll">
      <table>
      <thead>
      <tr>
      <th>Cliente</th>
      <th>Correo electrónico</th>
      <th>Teléfono</th>
      <th>Estado</th>
      </tr>
      </thead>
      <tbody>{data.data?.content.map(c => <tr key={c.id}>
      <td>
      <strong>{c.name}</strong>
      </td>
      <td>{c.email || '—'}</td>
      <td>{c.phone || '—'}</td>
      <td>
      <Status active={c.active} label={c.active ? 'Activo' : 'Inactivo'} />
      </td>
      </tr>)}</tbody>
      </table>
      </div>
      </ErrorOrLoading>
      </Panel>
      </>; }

export function Notifications() { const qc = useQueryClient();
  const data = useQuery({ queryKey: ['notifications'], queryFn: () => request<Notification[]>('/notifications') });
  const [error, setError] = useState('');
  async function mark(id: string) { try { await request(`/notifications/${id}/read`, { method: 'POST' }); await qc.invalidateQueries({ queryKey: ['notifications'] });
    } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar'); } } return <>
      <Header title="Notificaciones" subtitle="Mantente al día con las actividades de tu equipo." />
      <Panel>{error && <Alert>{error}</Alert>}<ErrorOrLoading loading={data.isLoading} error={data.error}>
      <div className="notification-list">{data.data?.map(n => <div className="notification-row" key={n.id}>
      <span className="notification-icon">
      <Bell01 size={19} />
      </span>
      <div>
      <strong>{n.title}</strong>
      <p>{n.message}</p>
      </div>{!n.readAt && <Button variant="secondary" onPress={() => mark(n.id)}>Marcar leída <CheckCircle size={16} />
      </Button>}</div>)}{data.data?.length === 0 && <Empty>No tienes notificaciones.</Empty>}</div>
      </ErrorOrLoading>
      </Panel>
      </>; }

export function Dashboard() { const orders = useQuery({ queryKey: ['dashboard-orders'], queryFn: () => request<Page<WorkOrder>>('/work-orders?page=0&size=20') });
  const customers = useQuery({ queryKey: ['dashboard-customers'], queryFn: () => request<Page<Customer>>('/customers?page=0&size=1') });
  return <>
      <Header title="Resumen operativo" subtitle="Una vista general de la actividad de tu empresa." />
      <div className="stats-grid">
      <Panel className="stat">
      <span>Órdenes totales</span>
      <strong>{orders.data?.totalElements ?? '—'}</strong>
      <small>Registradas en el sistema</small>
      </Panel>
      <Panel className="stat">
      <span>Clientes registrados</span>
      <strong>{customers.data?.totalElements ?? '—'}</strong>
      <small>En tu empresa</small>
      </Panel>
      <Panel className="stat">
      <span>Órdenes de la página</span>
      <strong>{orders.data?.content.length ?? '—'}</strong>
      <small>Mostradas actualmente</small>
      </Panel>
      </div>{(orders.error || customers.error) && <Alert>No se pudieron cargar todos los indicadores. Revisa los permisos del usuario.</Alert>}<Panel className="welcome-panel">
      <div>
      <span className="eyebrow">SERVICEEVIDENCE</span>
      <h2>Todo lo que necesitas para gestionar tus servicios.</h2>
      <p>Organiza las órdenes de trabajo, consulta clientes y mantén la trazabilidad de cada servicio.</p>
      </div>
      <ClipboardCheck size={90} />
      </Panel>
      </>; }
