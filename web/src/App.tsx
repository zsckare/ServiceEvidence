import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { auth, getAccessToken, setAccessToken, type User } from './api';
import { Login } from './features/auth/Login';
import { LandingPage } from './features/marketing/LandingPage';
import { AdminChoice, PlatformDashboard, TenantPicker } from './features/platform/PlatformPages';
import { Shell } from './components/layout/Shell';

type Mode = 'choice' | 'picker' | 'platform' | 'tenant';
type SelectedTenant = { id: string; name: string };

/** EN: Separates public pages from authenticated tenant/platform contexts.
 *  ES: Separa las páginas públicas de los contextos autenticados.
 */
export default function App() {
    const [user, setUser] = useState<User | null>(null);
    const [tenant, setTenant] = useState<SelectedTenant | null>(null);
    const [mode, setMode] = useState<Mode>('choice');
    const [baseToken, setBaseToken] = useState<string | null>(null);
    const queryClient = useQueryClient();
    const navigate = useNavigate();
    const authenticated = Boolean(user && getAccessToken());

    function logout() {
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
        navigate(next === 'platform' ? '/platform' : '/select-tenant');
    }

    async function handleLogin(token: string, authenticatedUser: User) {
        setUser(authenticatedUser);
        setBaseToken(token);
        setTenant(null);
        setMode(authenticatedUser.platformAdmin ? 'choice' : 'picker');
        queryClient.clear();
        navigate('/select-tenant');
    }

    async function selectTenant(id: string, name: string) {
        if (baseToken) setAccessToken(baseToken);
        const response = await auth.select(id);
        setAccessToken(response.accessToken);
        setTenant({ id, name });
        setMode('tenant');
        queryClient.clear();
        navigate('/app');
    }

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
