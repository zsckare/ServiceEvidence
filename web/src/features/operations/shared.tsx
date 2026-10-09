import type { ReactNode } from 'react';
import { Alert, Button } from '../../components/ui/primitives';

export function OperationError({ error }: { error: unknown }) {
    return error ? <Alert>{error instanceof Error ? error.message : 'Ocurrió un error.'}</Alert> : null;
}

export function FormActions({ busy, onCancel }: { busy: boolean; onCancel: () => void }) {
    return (
        <div className="operation-actions">
            <Button type="submit" isDisabled={busy}>{busy ? 'Guardando…' : 'Guardar cambios'}</Button>
            <Button type="button" variant="secondary" onPress={onCancel}>Cancelar</Button>
        </div>
    );
}

export function FormField({
    label, value, onChange, required, type = 'text', children,
}: {
    label: string;
    value?: string;
    onChange?: (value: string) => void;
    required?: boolean;
    type?: string;
    children?: ReactNode;
}) {
    return (
        <label className="operation-field">
            <span>{label}</span>
            {children ?? (
                <input
                    className="se-input"
                    type={type}
                    required={required}
                    value={value ?? ''}
                    onChange={(event) => onChange?.(event.target.value)}
                />
            )}
        </label>
    );
}
