import { FinancialDashboard } from './FinancialDashboard';
import { SubscriptionManagement } from './SubscriptionManagement';
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


/** EN: Platform console, isolated from tenant operational routes.
 * ES: Consola global independiente de las rutas operativas de empresas.
 */
export function PlatformDashboard({ onChooseTenant, onLogout }: { onChooseTenant: () => void; onLogout: () => void }) {
  const client = useQueryClient();
  const [section, setSection] = useState<'overview' | 'tenants' | 'subscriptions' | 'finance'>('overview');
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'all' | 'active' | 'inactive'>('all');
  const [creating, setCreating] = useState(false);
  const [mobileMenu, setMobileMenu] = useState(false);
  const [newTenant, setNewTenant] = useState({ name: '', code: '', adminName: '', adminEmail: '', adminPassword: '' });
  const tenants = useQuery({ queryKey: ['platform-tenants'], queryFn: platform.tenants });
  const create = useMutation({
    mutationFn: () => request<TenantInfo>('/platform/tenants', { method: 'POST', body: JSON.stringify(newTenant) }),
    onSuccess: async () => {
      setCreating(false);
      setNewTenant({ name: '', code: '', adminName: '', adminEmail: '', adminPassword: '' });
      await client.invalidateQueries({ queryKey: ['platform-tenants'] });
    },
  });
  const toggle = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => request<TenantInfo>(`/platform/tenants/${id}/status`, { method: 'PATCH', body: JSON.stringify({ active }) }),
    onSuccess: async () => { await client.invalidateQueries({ queryKey: ['platform-tenants'] }); },
  });
  const all = tenants.data ?? [];
  const active = all.filter(tenant => tenant.active).length;
  const inactive = all.length - active;
  const visible = all.filter(tenant =>
    (filter === 'all' || (filter === 'active' ? tenant.active : !tenant.active)) &&
    `${tenant.name} ${tenant.code}`.toLocaleLowerCase().includes(search.trim().toLocaleLowerCase()),
  );
  const labels = { overview: 'Resumen general', tenants: 'Empresas', subscriptions: 'Suscripciones', finance: 'Finanzas' };
  const navigation = [
    { key: 'overview' as const, label: 'Resumen general', symbol: '◫' },
    { key: 'tenants' as const, label: 'Empresas', symbol: '▦' },
    { key: 'subscriptions' as const, label: 'Planes y consumo', symbol: '◈' },
    { key: 'finance' as const, label: 'Finanzas', symbol: '▤' },
  ];
  function navigate(next: typeof section) { setSection(next); setMobileMenu(false); }

  const companyTable = <section className="console-panel" aria-label="Directorio de empresas">
    <div className="console-panel__header">
      <div><span className="console-overline">DIRECTORIO</span><h2>Empresas registradas</h2><p>Consulta el estado de cada espacio de trabajo.</p></div>
      <span className="console-count">{visible.length} resultados</span>
    </div>
    <div className="console-toolbar">
      <label className="console-search"><SearchLg size={18} /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="Buscar por empresa o clave" aria-label="Buscar empresas" /></label>
      <select className="console-filter" aria-label="Filtrar empresas por estado" value={filter} onChange={event => setFilter(event.target.value as typeof filter)}>
        <option value="all">Todos los estados</option><option value="active">Activas</option><option value="inactive">Suspendidas</option>
      </select>
    </div>
    <ErrorOrLoading loading={tenants.isLoading} error={tenants.error}>
      <div className="console-table-wrap"><table className="console-table"><thead><tr><th>Empresa</th><th>Identificador</th><th>Estado</th><th className="console-table__action">Administración</th></tr></thead>
      <tbody>{visible.map(tenant => <tr key={tenant.id}>
        <td><div className="console-company"><span className="console-company__icon"><Building07 size={20}/></span><div><strong>{tenant.name}</strong><small>Espacio de trabajo</small></div></div></td>
        <td><code>{tenant.code}</code></td><td><span className={`console-status ${tenant.active ? 'console-status--active' : 'console-status--inactive'}`}><span/>{tenant.active ? 'Activa' : 'Suspendida'}</span></td>
        <td className="console-table__action"><button type="button" className="console-link" disabled={toggle.isPending} onClick={() => {
          if (window.confirm(`¿${tenant.active ? 'Suspender' : 'Activar'} la empresa «${tenant.name}»?`)) toggle.mutate({ id: tenant.id, active: !tenant.active });
        }}>{tenant.active ? 'Suspender' : 'Reactivar'}</button></td>
      </tr>)}{visible.length === 0 && <tr><td colSpan={4}><div className="console-no-results">No hay empresas que coincidan con los filtros.</div></td></tr>}</tbody></table></div>
    </ErrorOrLoading>
    {toggle.error && <div className="console-message"><Alert>{toggle.error.message}</Alert></div>}
  </section>;

  return <div className="console-layout">
    {mobileMenu && <button className="console-backdrop" aria-label="Cerrar navegación" onClick={() => setMobileMenu(false)} />}
    <aside className={`console-sidebar ${mobileMenu ? 'console-sidebar--open' : ''}`}>
      <div className="console-sidebar__brand"><Brand /><span className="console-sidebar__edition">PLATFORM CONSOLE</span></div>
      <div className="console-sidebar__group"><span>WORKSPACE</span>
        {navigation.map(item => <button key={item.key} type="button" className={`console-nav ${section === item.key ? 'console-nav--active' : ''}`} onClick={() => navigate(item.key)}><span className="console-nav__symbol">{item.symbol}</span>{item.label}</button>)}
      </div>
      <div className="console-sidebar__bottom"><div className="console-sidebar__identity"><span className="console-sidebar__avatar"><Shield01 size={20}/></span><div><strong>Administrador global</strong><small>Acceso a la plataforma</small></div></div>
        <button type="button" onClick={onChooseTenant} className="console-sidebar__button"><Building07 size={17}/> Entrar a una empresa</button>
        <button type="button" onClick={onLogout} className="console-sidebar__button"><LogOut01 size={17}/> Cerrar sesión</button>
      </div>
    </aside>
    <div className="console-main">
      <header className="console-topbar"><button type="button" className="console-menu" onClick={() => setMobileMenu(true)} aria-label="Abrir navegación">☰</button><div className="console-breadcrumb">ServiceEvidence <span>/</span> Plataforma <span>/</span> <strong>{labels[section]}</strong></div><span className="console-topbar__role"><Shield01 size={15}/> SUPER_ADMIN</span></header>
      <main className="console-content">
        <div className="console-heading"><div><span className="console-overline">ADMINISTRACIÓN GLOBAL</span><h1>{labels[section]}</h1><p>{section === 'overview' ? 'Visibilidad y control de todos los espacios de trabajo.' : section === 'tenants' ? 'Gestiona las empresas que utilizan ServiceEvidence.' : 'Administra los planes y supervisa el consumo por empresa.'}</p></div>
          {section !== 'subscriptions' && <button className="console-primary" type="button" onClick={() => { setCreating(true); setSection('tenants'); }}>+ Nueva empresa</button>}
        </div>
        {section === 'overview' && <>
          <div className="console-hero"><div><span className="console-hero__eyebrow">CENTRO DE CONTROL</span><h2>Tu plataforma, en perspectiva.</h2><p>Supervisa la actividad de tus empresas y administra su acceso desde un solo lugar.</p><button type="button" onClick={() => navigate('tenants')}>Explorar empresas <ArrowRight size={16}/></button></div><div className="console-hero__visual" aria-hidden="true"><span>SE</span><div className="console-hero__orbit"/></div></div>
          <div className="console-kpis"><div className="console-kpi"><span>Empresas registradas</span><strong>{tenants.isLoading ? '—' : all.length.toLocaleString()}</strong><small>Total de espacios creados</small><span className="console-kpi__mark">▦</span></div>
          <div className="console-kpi"><span>Empresas activas</span><strong>{tenants.isLoading ? '—' : active.toLocaleString()}</strong><small>Con acceso habilitado</small><span className="console-kpi__mark">✓</span></div>
          <div className="console-kpi"><span>Empresas suspendidas</span><strong>{tenants.isLoading ? '—' : inactive.toLocaleString()}</strong><small>Acceso temporalmente bloqueado</small><span className="console-kpi__mark">◷</span></div></div>
          <div className="console-section-title"><div><h2>Vista general de empresas</h2><p>Directorio y estado actual de tus tenants.</p></div><button type="button" onClick={() => navigate('tenants')}>Ver directorio completo <ArrowRight size={16}/></button></div>
          {companyTable}
        </>}
        {section === 'tenants' && <>
          {creating && <section className="console-panel console-create"><div className="console-panel__header"><div><span className="console-overline">NUEVO TENANT</span><h2>Registrar empresa</h2><p>Crea un espacio de trabajo y su administrador inicial.</p></div><button type="button" className="console-link" onClick={() => setCreating(false)}>Cancelar</button></div>
            <form className="console-form" onSubmit={event => { event.preventDefault(); create.mutate(); }}>
              {([['name', 'Nombre de la empresa'], ['code', 'Clave única'], ['adminName', 'Nombre del administrador'], ['adminEmail', 'Correo del administrador'], ['adminPassword', 'Contraseña inicial']] as const).map(([field, label]) => <label key={field}>{label}<input className="se-input" required minLength={field === 'adminPassword' ? 12 : undefined} type={field === 'adminPassword' ? 'password' : field === 'adminEmail' ? 'email' : 'text'} value={newTenant[field]} onChange={event => setNewTenant(previous => ({ ...previous, [field]: event.target.value }))}/></label>)}
              <div className="console-form__footer"><span>La contraseña debe tener al menos 12 caracteres.</span><button className="console-primary" type="submit" disabled={create.isPending}>{create.isPending ? 'Creando…' : 'Crear empresa'}</button></div>
              {create.error && <Alert>{create.error.message}</Alert>}
            </form></section>}
          <div className="console-kpis console-kpis--compact"><div className="console-kpi"><span>Total</span><strong>{all.length}</strong></div><div className="console-kpi"><span>Activas</span><strong>{active}</strong></div><div className="console-kpi"><span>Suspendidas</span><strong>{inactive}</strong></div></div>
          {companyTable}
        </>}
        {section === 'subscriptions' && <div className="console-subscriptions"><SubscriptionManagement tenants={all}/></div>}
        {section === 'finance' && <FinancialDashboard />}
      </main>
    </div>
  </div>;
}
