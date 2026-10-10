import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { auth, getAccessToken, setAccessToken, type User } from './api';
import { Login } from './features/auth/Login';
import { LandingPage } from './features/marketing/LandingPage';
import { AdminChoice, PlatformDashboard, TenantPicker } from './features/platform/PlatformPages';
import { Shell } from './components/layout/Shell';
import { readSession, saveSession, type SessionContext } from './session';

type Mode = 'choice' | 'picker' | 'platform' | 'tenant';
type SelectedTenant = { id: string; name: string };

/** EN: Separates public pages from authenticated tenant/platform contexts.
 *  ES: Separa las páginas públicas de los contextos autenticados.
 */
export default function App() {
    const [restored] = useState(() => readSession());
    const [restoring, setRestoring] = useState(Boolean(restored));
    const [user, setUser] = useState<User | null>(restored?.user ?? null);
    const [tenant, setTenant] = useState<SelectedTenant | null>(restored?.tenant ?? null);
    const [mode, setMode] = useState<Mode>(restored?.mode ?? 'choice');
    const [baseToken, setBaseToken] = useState<string | null>(restored?.baseToken ?? null);
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    // EN: Restore the bearer token before the first render.
    // ES: Restaura el token antes del primer renderizado.
    if (restored && !getAccessToken()) setAccessToken(restored.activeToken);
    const authenticated = Boolean(user && getAccessToken());

    useEffect(() => {
        if (!restored) return;
        let cancelled = false;
        auth.me().then((freshUser) => {
            if (cancelled) return;
            setUser(freshUser);
            setRestoring(false);
        }).catch(() => {
            if (cancelled) return;
            setAccessToken(null);
            saveSession(null);
            setUser(null);
            setTenant(null);
            setBaseToken(null);
            setRestoring(false);
        });
        return () => { cancelled = true; };
        // EN: Validate once on initial mount, including React Strict Mode.
        // ES: Valida al montar, también con React Strict Mode.
    }, []);

    useEffect(() => {
        if (restoring) return;
        if (!user || !baseToken || !getAccessToken()) {
            saveSession(null);
            return;
        }
        const state: SessionContext = {
            user, baseToken, activeToken: getAccessToken()!, tenant, mode,
        };
        saveSession(state);
    }, [user, baseToken, tenant, mode, restoring]);

    function logout() {
        saveSession(null);
        setAccessToken(null);
        setBaseToken(null);
        setUser(null);
        setTenant(null);
        setMode('choice');
        queryClient.clear();
        navigate('/');
    }

    function switchContext(next: 'choice' | 'picker' | 'platform') {
        if (baseToken) setAccessToken(baseToken);
        setTenant(null);
        setMode(next);
        queryClient.clear();
        saveSession({ user: user!, baseToken: baseToken!, activeToken: baseToken!, tenant: null, mode: next });
        navigate(next === 'platform' ? '/platform' : '/select-tenant');
    }

    async function handleLogin(token: string, authenticatedUser: User) {
        setUser(authenticatedUser);
        setBaseToken(token);
        setTenant(null);
        setMode(authenticatedUser.platformAdmin ? 'choice' : 'picker');
        queryClient.clear();
        saveSession({ user: authenticatedUser, baseToken: token, activeToken: token, tenant: null, mode: authenticatedUser.platformAdmin ? 'choice' : 'picker' });
        navigate('/select-tenant');
    }

    async function selectTenant(id: string, name: string) {
        if (baseToken) setAccessToken(baseToken);
        const response = await auth.select(id);
        setAccessToken(response.accessToken);
        setTenant({ id, name });
        setMode('tenant');
        queryClient.clear();
        saveSession({ user: user!, baseToken: baseToken!, activeToken: response.accessToken, tenant: { id, name }, mode: 'tenant' });
        navigate('/app');
    }

    if (restoring) return <div className="session-restoring" role="status">Restaurando sesión…</div>;

    return (
        <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route
                path="/login"
                element={authenticated
                    ? <Navigate to={mode === 'tenant' ? '/app' : '/select-tenant'} replace />
                    : <Login onLogin={handleLogin} />}
            />
            <Route
                path="/select-tenant"
                element={!authenticated
                    ? <Navigate to="/login" replace />
                    : user!.platformAdmin && mode === 'choice'
                        ? <AdminChoice
                            onGlobal={() => switchContext('platform')}
                            onTenant={() => switchContext('picker')}
                            onLogout={logout}
                        />
                        : <TenantPicker
                            isAdmin={user!.platformAdmin}
                            onBack={user!.platformAdmin ? () => switchContext('choice') : undefined}
                            onSelected={selectTenant}
                        />}
            />
            <Route
                path="/platform"
                element={!authenticated
                    ? <Navigate to="/login" replace />
                    : !user!.platformAdmin
                        ? <Navigate to="/select-tenant" replace />
                        : <PlatformDashboard
                            onChooseTenant={() => switchContext('picker')}
                            onLogout={logout}
                        />}
            />
            <Route
                path="/app/*"
                element={!authenticated
                    ? <Navigate to="/login" replace />
                    : !tenant
                        ? <Navigate to="/select-tenant" replace />
                        : <Shell
                            user={user!}
                            tenant={tenant.name}
                            onLogout={logout}
                            onSwitch={user!.platformAdmin ? () => switchContext('choice') : undefined}
                        />}
            />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}
