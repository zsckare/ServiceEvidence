import { useState, type FormEvent } from 'react';
import { ArrowRight, Shield01 } from '@untitledui/icons';
import { auth, setAccessToken, type User } from '../../api';
import { Alert, Brand, Button, Field } from '../../components/ui/primitives';

type LoginProps = {
    onLogin: (token: string, user: User) => Promise<void>;
};

/** EN: Authenticate and load the user before choosing a tenant.
 *  ES: Autentica y obtiene el usuario antes de seleccionar una empresa.
 */
export function Login({ onLogin }: LoginProps) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');

    async function submit(event: FormEvent) {
        event.preventDefault();
        setBusy(true);
        setError('');

        try {
            const pair = await auth.login(email, password);
            setAccessToken(pair.accessToken);
            const user = await auth.me();
            await onLogin(pair.accessToken, user);
        } catch (cause) {
            setAccessToken(null);
            setError(cause instanceof Error ? cause.message : 'Error de autenticación');
        } finally {
            setBusy(false);
        }
    }

    return (
        <div className="auth-layout">
            <div className="auth-intro">
                <Brand />
                <div className="auth-hero">
                    <span className="eyebrow">OPERACIONES INTELIGENTES</span>
                    <h2>Tu operación, bajo control.</h2>
                    <p>Gestiona órdenes, equipos de trabajo y evidencias desde un mismo lugar.</p>
                    <div className="hero-line" />
                </div>
                <span className="auth-footer">© ServiceEvidence</span>
            </div>

            <main className="auth-main">
                <form className="login-form" onSubmit={submit}>
                    <div className="mobile-brand"><Brand /></div>
                    <div className="login-symbol"><Shield01 size={25} /></div>
                    <h1>Bienvenido de nuevo</h1>
                    <p className="muted">Ingresa tus credenciales para continuar.</p>

                    <div className="form-stack">
                        <Field
                            label="Correo electrónico"
                            type="email"
                            required
                            autoComplete="username"
                            placeholder="nombre@empresa.com"
                            value={email}
                            onChange={setEmail}
                        />
                        <Field
                            label="Contraseña"
                            type="password"
                            required
                            autoComplete="current-password"
                            placeholder="Tu contraseña"
                            value={password}
                            onChange={setPassword}
                        />
                        {error && <Alert>{error}</Alert>}
                        <Button type="submit" isDisabled={busy} className="full-width">
                            {busy ? 'Ingresando…' : 'Iniciar sesión'}
                            <ArrowRight size={17} />
                        </Button>
                    </div>
                </form>
            </main>
        </div>
    );
}
