import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { request, type TenantInfo } from '../../api';
import { Alert, Button, Empty, Panel } from '../../components/ui/primitives';

// EN: The API exposes entitlements, not billing or payment status.
// ES: La API expone cuotas, no información de pagos o facturación.
type Plan = { code: string; name: string; maxUsers: number; maxWorkOrdersPerMonth: number };
type Quota = { used: number; limit: number };
type Usage = {
    tenantId: string;
    planCode: string;
    planName: string;
    users: Quota;
    monthlyWorkOrders: Quota;
    monthUtc: string;
};

function QuotaMeter({ title, quota }: { title: string; quota: Quota }) {
    const percent = quota.limit > 0 ? Math.round((quota.used / quota.limit) * 100) : 0;
    const warning = percent >= 80;
    return (
        <div className="subscription-meter">
            <div className="subscription-meter__heading">
                <span>{title}</span>
                <strong>{quota.used.toLocaleString()} / {quota.limit.toLocaleString()}</strong>
            </div>
            <div className="subscription-meter__track" role="progressbar" aria-label={title}
                aria-valuenow={quota.used} aria-valuemin={0} aria-valuemax={quota.limit}>
                <div className={warning ? 'subscription-meter__fill subscription-meter__fill--warning' : 'subscription-meter__fill'}
                    style={{ width: `${Math.min(100, percent)}%` }} />
            </div>
            {warning && <small>{percent >= 100 ? 'Cuota alcanzada' : 'Próximo al límite'} ({percent}%)</small>}
        </div>
    );
}

/** EN: Platform-only subscription management for a selected tenant.
 * ES: Administración de suscripciones de una empresa para SUPER_ADMIN.
 */
export function SubscriptionManagement({ tenants }: { tenants: TenantInfo[] }) {
    const [tenantId, setTenantId] = useState('');
    const [planCode, setPlanCode] = useState('');
    const [success, setSuccess] = useState('');
    const client = useQueryClient();
    const plans = useQuery({ queryKey: ['subscription-plans'], queryFn: () => request<Plan[]>('/subscriptions/plans') });
    const usage = useQuery({
        queryKey: ['platform-subscription-usage', tenantId],
        queryFn: () => request<Usage>(`/subscriptions/platform/tenants/${tenantId}/usage`),
        enabled: Boolean(tenantId),
    });
    const change = useMutation({
        mutationFn: (code: string) => request(`/subscriptions/platform/tenants/${tenantId}`, {
            method: 'PUT', body: JSON.stringify({ planCode: code }),
        }),
        onSuccess: async () => {
            setSuccess('El plan se actualizó correctamente.');
            await client.invalidateQueries({ queryKey: ['platform-subscription-usage', tenantId] });
        },
    });

    return <Panel>
        <div className="panel-heading">
            <h2>Planes y consumo</h2>
            <p>Consulta el consumo de una empresa y modifica su plan comercial.</p>
        </div>
        <div className="subscription-controls">
            <label>Empresa
                <select className="se-input" value={tenantId} onChange={event => {
                    setTenantId(event.target.value); setPlanCode(''); setSuccess('');
                }}>
                    <option value="">Selecciona una empresa</option>
                    {tenants.map(tenant => <option key={tenant.id} value={tenant.id}>{tenant.name}</option>)}
                </select>
            </label>
        </div>
        {usage.isLoading && <p>Cargando consumo…</p>}
        {usage.error && <Alert>{usage.error.message}</Alert>}
        {!tenantId && <Empty>Selecciona una empresa para consultar su suscripción.</Empty>}
        {usage.data && <div className="subscription-details">
            <div><strong>Plan actual: {usage.data.planName}</strong>
                <p>Órdenes contabilizadas durante {usage.data.monthUtc} (UTC).</p></div>
            <QuotaMeter title="Usuarios activos" quota={usage.data.users} />
            <QuotaMeter title="Órdenes creadas este mes" quota={usage.data.monthlyWorkOrders} />
            <div className="subscription-controls">
                <label>Cambiar plan
                    <select className="se-input" value={planCode || usage.data.planCode}
                        onChange={event => { setPlanCode(event.target.value); setSuccess(''); }}>
                        {(plans.data ?? []).map(plan => <option key={plan.code} value={plan.code}>
                            {plan.name} — {plan.maxUsers} usuarios / {plan.maxWorkOrdersPerMonth} órdenes al mes
                        </option>)}
                    </select>
                </label>
                <Button isDisabled={change.isPending || !plans.data?.length || (planCode || usage.data.planCode) === usage.data.planCode}
                    onPress={() => {
                        const selected = planCode || usage.data.planCode;
                        if (window.confirm(`¿Cambiar el plan a ${selected}?`)) change.mutate(selected);
                    }}>Guardar plan</Button>
            </div>
            {change.error && <Alert>{change.error.message}</Alert>}
            {success && <p role="status">{success}</p>}
        </div>}
    </Panel>;
}
