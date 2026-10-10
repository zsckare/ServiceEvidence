import { useState } from 'react';
import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { Bell01, Building07, ClipboardCheck, Home01, LogOut01, Menu01, Users01, XClose } from '@untitledui/icons';
import type { User } from '../../api';
import { Brand, Button } from '../ui/primitives';
import { Dashboard, Notifications } from '../../features/operations/OperationsPages';
import { CustomersPage } from '../../features/operations/CustomersPage';
import { LocationsPage } from '../../features/operations/LocationsPage';
import { OrdersPage } from '../../features/operations/OrdersPage';
import { OrderDetailPage } from '../../features/operations/OrderDetailPage';
import { UsersPage } from '../../features/admin/UsersPage';
import { SettingsPage } from '../../features/admin/SettingsPage';
import { SecurityAuditPage } from '../../features/admin/SecurityAuditPage';

const menu = [
    { to: '/app', label: 'Resumen', icon: Home01 },
    { to: '/app/orders', label: 'Órdenes de trabajo', icon: ClipboardCheck },
    { to: '/app/customers', label: 'Clientes', icon: Users01 },
    { to: '/app/notifications', label: 'Notificaciones', icon: Bell01 },
    { to: '/app/users', label: 'Usuarios y roles', icon: Users01 },
    { to: '/app/settings', label: 'Configuración', icon: Building07 },
    { to: '/app/security/audit', label: 'Auditoría', icon: ClipboardCheck },
];

export function Shell({ user, tenant, onLogout, onSwitch }: { user: User; tenant: string; onLogout: () => void; onSwitch?: () => void }) {
  const [open, setOpen] = useState(false);
  const sidebar = <>
      <Brand />
      <div className="sidebar-tenant">
      <span>EMPRESA ACTUAL</span>
      <strong>
      <Building07 size={17} /> {tenant}</strong>
      </div>
      <nav className="side-links">
      <div className="nav-label">GENERAL</div>{menu.map(item => <NavLink end key={item.to} to={item.to} className={({ isActive }) => `nav-item ${isActive ? 'nav-item--active' : ''}`} onClick={() => setOpen(false)}>
      <item.icon size={19} />{item.label}</NavLink>)}</nav>
      <div className="sidebar-bottom">
      <div className="user-chip">
      <div className="user-avatar">{user.fullName.charAt(0).toUpperCase()}</div>
      <div>
      <strong>{user.fullName}</strong>
      <small>{user.email}</small>
      </div>
      </div>{onSwitch && <Button variant="ghost" onPress={onSwitch}>
      <Building07 size={18} /> Cambiar contexto</Button>}<Button variant="ghost" onPress={onLogout}>
      <LogOut01 size={18} /> Cerrar sesión</Button>
      </div>
      </>;
  return <div className="workspace">
      <aside className="sidebar sidebar--desktop">{sidebar}</aside>{open && <div className="mobile-overlay" onClick={() => setOpen(false)}>
      <aside className="sidebar sidebar--mobile" onClick={e => e.stopPropagation()}>{sidebar}</aside>
      </div>}<div className="content-column">
      <header className="topbar">
      <Button variant="ghost" className="mobile-menu" onPress={() => setOpen(true)} aria-label="Abrir menú">
      <Menu01 size={22} />
      </Button>
      <span className="topbar-location">
      <Building07 size={16} /> {tenant}</span>
      <div className="topbar-user">{user.fullName}</div>{open && <Button variant="ghost" className="mobile-menu" onPress={() => setOpen(false)} aria-label="Cerrar menú">
      <XClose size={20} />
      </Button>}</header>
      <main className="main-content">
      <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/orders" element={<OrdersPage />} />
      <Route path="/orders/:orderId" element={<OrderDetailPage user={user} />} />
      <Route path="/customers" element={<CustomersPage />} />
      <Route path="/customers/:customerId/locations" element={<LocationsPage />} />
      <Route path="/notifications" element={<Notifications />} />
      <Route path="/users" element={<UsersPage />} />
      <Route path="/settings" element={<SettingsPage />} />
      <Route path="/security/audit" element={<SecurityAuditPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      </main>
      </div>
      </div>;
}
