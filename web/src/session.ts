import type { User } from './api';

/** EN: Per-tab session state. ES: Estado de sesión por pestaña. */
export type SessionContext = {
    user: User;
    baseToken: string;
    activeToken: string;
    tenant: { id: string; name: string } | null;
    mode: 'choice' | 'picker' | 'platform' | 'tenant';
};
const KEY = 'serviceevidence.session.v1';

export function readSession(): SessionContext | null {
    try {
        const raw = sessionStorage.getItem(KEY);
        if (!raw) return null;
        const data: unknown = JSON.parse(raw);
        if (!data || typeof data !== 'object') return null;
        const value = data as Partial<SessionContext>;
        if (!value.user?.id || !value.baseToken || !value.activeToken ||
            !['choice', 'picker', 'platform', 'tenant'].includes(value.mode ?? '') ||
            (value.mode === 'tenant' && !value.tenant?.id)) return null;
        return value as SessionContext;
    } catch {
        return null;
    }
}

export function saveSession(value: SessionContext | null): void {
    try {
        if (value) sessionStorage.setItem(KEY, JSON.stringify(value));
        else sessionStorage.removeItem(KEY);
    } catch {
        // EN: Storage may be unavailable in restricted browser contexts.
        // ES: El almacenamiento puede estar deshabilitado en algunos navegadores.
    }
}
