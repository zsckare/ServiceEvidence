import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { auth, getAccessToken, setAccessToken, type User } from './api';
import { Login } from './features/auth/Login';
import { AdminChoice, PlatformDashboard, TenantPicker } from './features/platform/PlatformPages';
import { Shell } from './components/layout/Shell';

type Mode = 'choice' | 'picker' | 'platform' | 'tenant';
type SelectedTenant = { id: string; name: string };

/** EN: Coordinates the authenticated user and tenant context.
 *  ES: Coordina la sesión y el contexto de empresa seleccionado.
 */
export default function App() {
    const [user, setUser] = useState<User | null>(null);
    const [tenant, setTenant] = useState<SelectedTenant | null>(null);
    const [mode, setMode] = useState<Mode>('choice');
    const [baseToken, setBaseToken] = useState<string | null>(null);
    const queryClient = useQueryClient();
    const navigate = useNavigate();

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
        navigate('/');
    }

    async function handleLogin(token: string, authenticatedUser: User) {
        setUser(authenticatedUser);
        setBaseToken(token);
        setTenant(null);
        setMode(authenticatedUser.platformAdmin ? 'choice' : 'picker');
        queryClient.clear();
    }

    async function selectTenant(id: string, name: string) {
        if (baseToken) setAccessToken(baseToken);
        const response = await auth.select(id);
        setAccessToken(response.accessToken);
        setTenant({ id, name });
        setMode('tenant');
        queryClient.clear();
        navigate('/');
    }

    if (!user || !getAccessToken()) {
        return <Login onLogin={handleLogin} />;
    }

    if (user.platformAdmin && mode === 'choice') {
        return (
            <AdminChoice
                onGlobal={() => switchContext('platform')}
                onTenant={() => switchContext('picker')}
                onLogout={logout}
            />
        );
    }

    if (user.platformAdmin && mode === 'platform') {
        return (
            <PlatformDashboard
                onChooseTenant={() => switchContext('picker')}
                onLogout={logout}
            />
        );
    }

    if (mode === 'picker' || !tenant) {
        return (
            <TenantPicker
                isAdmin={user.platformAdmin}
                onBack={user.platformAdmin ? () => switchContext('choice') : undefined}
                onSelected={selectTenant}
            />
        );
    }

    return (
        <Shell
            user={user}
            tenant={tenant.name}
            onLogout={logout}
            onSwitch={user.platformAdmin ? () => switchContext('choice') : undefined}
        />
    );
}
