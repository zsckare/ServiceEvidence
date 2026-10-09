import type { ComponentProps, ReactNode } from 'react';
import { Button as AriaButton, Input, Label, TextField } from 'react-aria-components';
import { ClipboardCheck } from '@untitledui/icons';

export function Button({ children, variant = 'primary', className = '', ...props }: ComponentProps<typeof AriaButton> & { variant?: 'primary' | 'secondary' | 'ghost' }) {
    return (
        <AriaButton {...props} className={`se-button se-button--${variant} ${className}`}>
            {children}
        </AriaButton>
    );
}

export function Field({ label, value, onChange, type = 'text', placeholder, required = false, autoComplete }: {
    label: string;
    value: string;
    onChange: (value: string) => void;
    type?: string;
    placeholder?: string;
    required?: boolean;
    autoComplete?: string;
}) {
    return (
        <TextField className="se-field" isRequired={required} type={type} value={value} onChange={onChange}>
            <Label>{label}</Label>
            <Input autoComplete={autoComplete} placeholder={placeholder} className="se-input" />
        </TextField>
    );
}

export function Brand({ compact = false }: { compact?: boolean }) {
    return (
        <div className="brand">
            <div className="brand-mark"><ClipboardCheck size={22} /></div>
            {!compact && (
                <div>
                    <strong>ServiceEvidence</strong>
                    <small>Field service management</small>
                </div>
            )}
        </div>
    );
}

export function Alert({ children }: { children: ReactNode }) {
    return <div role="alert" className="se-alert">{children}</div>;
}

export function Panel({ children, className = '' }: { children: ReactNode; className?: string }) {
    return <section className={`se-panel ${className}`}>{children}</section>;
}

export function Header({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
    return (
        <div className="page-heading">
            <div>
                <h1>{title}</h1>
                {subtitle && <p>{subtitle}</p>}
            </div>
            {actions && <div className="heading-actions">{actions}</div>}
        </div>
    );
}

export function Loading() {
    return <div className="loading" role="status">Cargando información…</div>;
}

export function Empty({ children }: { children: ReactNode }) {
    return <div className="empty">{children}</div>;
}

export function Status({ active, label }: { active: boolean; label: string }) {
    return (
        <span className={`status ${active ? 'status--active' : 'status--muted'}`}>
            <span className="status-dot" />
            {label}
        </span>
    );
}

export function ErrorOrLoading({ loading, error, children }: { loading: boolean; error: Error | null; children: ReactNode }) {
    if (loading) return <Loading />;
    if (error) return <Alert>{error.message}</Alert>;
    return <>{children}</>;
}
