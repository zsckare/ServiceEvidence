import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, Building07, LogOut01, SearchLg, Shield01 } from '@untitledui/icons';
import { auth, platform, request, type TenantInfo } from '../../api';
import { Alert, Brand, Button, Empty, ErrorOrLoading, Header, Panel, Status } from '../../components/ui/primitives';

export function AdminChoice({ onGlobal, onTenant, onLogout }: { onGlobal: () => void; onTenant: () => void; onLogout: () => void }) {
  return <div className="entry-page">
      <div className="entry-top">
      <Brand />
      <Button variant="ghost" onPress={onLogout}>
      <LogOut01 size={17} /> Cerrar sesión</Button>
      </div>
      <div className="entry-body">
      <span className="eyebrow">ADMINISTRACIÓN</span>
      <Header title="¿Dónde deseas trabajar?" subtitle="Selecciona el espacio de ServiceEvidence al que quieres acceder." />
      <div className="choice-grid">
      <button type="button" className="choice-card" onClick={onGlobal}>
      <span className="choice-icon">
      <Shield01 size={25} />
      </span>
      <h2>Administración global</h2>
      <p>Consulta las empresas y administra la plataforma.</p>
      <span className="choice-action">Entrar al panel global <ArrowRight size={18} />
      </span>
      </button>
      <button type="button" className="choice-card" onClick={onTenant}>
      <span className="choice-icon">
      <Building07 size={25} />
      </span>
      <h2>Entrar a una empresa</h2>
      <p>Gestiona órdenes, clientes y actividades de un tenant.</p>
      <span className="choice-action">Seleccionar empresa <ArrowRight size={18} />
      </span>
      </button>
      </div>
      </div>
      </div>;
}

export function TenantPicker({ isAdmin, onSelected, onBack }: { isAdmin: boolean; onSelected: (id: string, name: string) => Promise<void>; onBack?: () => void }) {
  const ids = useQuery({ queryKey: ['available-tenants'], queryFn: auth.tenants });
  const details = useQuery({ queryKey: ['platform-tenants'], queryFn: platform.tenants, enabled: isAdmin });
  const [search, setSearch] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState('');
  const tenants: TenantInfo[] = (ids.data ?? []).map(id => details.data?.find(t => t.id === id) ?? { id, code: '', name: 'Empresa sin nombre disponible', active: true });
  const filtered = tenants.filter(t => `${t.name} ${t.code}`.toLowerCase().includes(search.toLowerCase()));
  async function select(t: TenantInfo) { setBusy(t.id); setError('');
    try { await onSelected(t.id, t.name);
    } catch (e) { setError(e instanceof Error ? e.message : String(e));
    } finally { setBusy(null); } }
  return <div className="entry-page">
      <div className="entry-top">
      <Brand />{onBack && <Button variant="ghost" onPress={onBack}>
      <ArrowLeft size={16} /> Volver</Button>}</div>
      <div className="entry-body">
      <span className="eyebrow">ESPACIO DE TRABAJO</span>
      <Header title="Selecciona una empresa" subtitle="Accede al espacio de trabajo de una empresa autorizada." />
      <Panel>
      <div className="search-box">
      <SearchLg size={18} />
      <input aria-label="Buscar empresas" value={search} onChange={e => setSearch(e.target.value)} placeholder="Buscar por nombre o clave…" />
      </div>
      <ErrorOrLoading loading={ids.isLoading || (isAdmin && details.isLoading)} error={(ids.error || (isAdmin ? details.error : null)) as Error | null}>
      <div className="tenant-list">{filtered.map(t => <div className="tenant-row" key={t.id}>
      <div className="tenant-avatar">
      <Building07 size={20} />
      </div>
      <div className="tenant-info">
      <strong>{t.name}</strong>
      <small>{t.code || 'Empresa autorizada'}</small>
      </div>
      <Status active={t.active} label={t.active ? 'Activa' : 'Inactiva'} />
      <Button isDisabled={!!busy || !t.active} onPress={() => select(t)}>{busy === t.id ? 'Entrando…' : 'Entrar'} <ArrowRight size={16} />
      </Button>
      </div>)}{filtered.length === 0 && <Empty>No se encontraron empresas.</Empty>}</div>
      </ErrorOrLoading>{error && <Alert>{error}</Alert>}</Panel>
      </div>
      </div>;
}

export function PlatformDashboard({ onChooseTenant, onLogout }: { onChooseTenant: () => void; onLogout: () => void }) {
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [newTenant, setNewTenant] = useState({ name: '', code: '', adminName: '', adminEmail: '', adminPassword: '' });
  const create = useMutation({
    mutationFn: () => request<TenantInfo>('/platform/tenants', { method: 'POST', body: JSON.stringify(newTenant) }),
    onSuccess: async () => { setCreating(false); setNewTenant({ name: '', code: '', adminName: '', adminEmail: '', adminPassword: '' }); await queryClient.invalidateQueries({ queryKey: ['platform-tenants'] }); },
  });
  const toggle = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => request<TenantInfo>(`/platform/tenants/${id}/status`, { method: 'PATCH', body: JSON.stringify({ active }) }),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['platform-tenants'] }); },
  });
  const data = useQuery({ queryKey: ['platform-tenants'], queryFn: platform.tenants });
  return <div className="workspace">
      <aside className="sidebar sidebar--desktop">
      <Brand />
      <nav className="side-links">
      <div className="nav-label">PLATAFORMA</div>
      <div className="nav-item nav-item--active">
      <Shield01 size={19} /> Empresas</div>
      </nav>
      <div className="sidebar-bottom">
      <Button variant="ghost" onPress={onLogout}>
      <LogOut01 size={18} /> Cerrar sesión</Button>
      </div>
      </aside>
      <main className="main-content">
      <Header title="Administración global" subtitle="Gestiona los espacios de trabajo de ServiceEvidence." actions={<Button onPress={onChooseTenant}>
      <Building07 size={17} /> Entrar a una empresa</Button>} />
      <div className="admin-platform-actions"><Button onPress={() => setCreating((value) => !value)}>{creating ? 'Cancelar' : 'Nueva empresa'}</Button></div>
      {creating && <Panel>
        <h2>Registrar empresa</h2>
        <form className="admin-form" onSubmit={(event) => { event.preventDefault(); create.mutate(); }}>
          {([
            ['name', 'Nombre de empresa'], ['code', 'Clave única'], ['adminName', 'Nombre del administrador'],
            ['adminEmail', 'Correo del administrador'], ['adminPassword', 'Contraseña inicial (mínimo 12 caracteres)'],
          ] as const).map(([field, label]) => <label key={field}>{label}
            <input className="se-input" required minLength={field === 'adminPassword' ? 12 : undefined}
              type={field === 'adminPassword' ? 'password' : field === 'adminEmail' ? 'email' : 'text'}
              value={newTenant[field]} onChange={(event) => setNewTenant({ ...newTenant, [field]: event.target.value })} />
          </label>)}
          {create.error && <Alert>{create.error.message}</Alert>}
          <Button type="submit" isDisabled={create.isPending}>Crear empresa</Button>
        </form>
      </Panel>}
      <div className="stats-grid">
      <Panel className="stat">
      <span>Empresas registradas</span>
      <strong>{data.data?.length ?? '—'}</strong>
      <small>Tenants en la plataforma</small>
      </Panel>
      <Panel className="stat">
      <span>Empresas activas</span>
      <strong>{data.data?.filter(t => t.active).length ?? '—'}</strong>
      <small>Disponibles para operar</small>
      </Panel>
      </div>
      {toggle.error && <Alert>{toggle.error.message}</Alert>}
      <Panel>
      <div className="panel-heading">
      <h2>Empresas</h2>
      <p>Listado de empresas registradas en la plataforma.</p>
      </div>
      <ErrorOrLoading loading={data.isLoading} error={data.error}>
      <div className="table-scroll">
      <table>
      <thead>
      <tr>
      <th>Empresa</th>
      <th>Clave</th>
      <th>Estado</th>
      <th>Acciones</th>
      </tr>
      </thead>
      <tbody>{data.data?.map(t => <tr key={t.id}>
      <td>
      <strong>{t.name}</strong>
      </td>
      <td>{t.code}</td>
      <td>
      <Status active={t.active} label={t.active ? 'Activa' : 'Inactiva'} />
      </td>
      <td><Button variant="secondary" isDisabled={toggle.isPending} onPress={() => {
        if (window.confirm(`¿${t.active ? 'Suspender' : 'Activar'} ${t.name}?`)) toggle.mutate({ id: t.id, active: !t.active });
      }}>{t.active ? 'Suspender' : 'Activar'}</Button></td>
      </tr>)}</tbody>
      </table>
      </div>
      </ErrorOrLoading>
      </Panel>
      </main>
      </div>;
}
