import './financial.css';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { request } from '../../api';
import { Alert } from '../../components/ui/primitives';

/** EN: Platform-only financial metrics. ES: Indicadores financieros exclusivos de la plataforma. */
type CurrencySummary = { currency: string; collectedMinor: number; invoicedMinor: number; outstandingMinor: number; overdueMinor: number; openInvoices: number; overdueInvoices: number };
type FinancialOverview = { from: string; to: string; currencies: CurrencySummary[]; subscriptions: { active: number; trialing: number; pastDue: number; canceled: number; expired: number; upcomingRenewals: number } };
const isoDate = (date: Date) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const today = () => isoDate(new Date());
const thirtyDaysAgo = () => { const d = new Date(); d.setDate(d.getDate()-29); return isoDate(d); };
const money = (minor: number, currency: string) => new Intl.NumberFormat('es-MX', { style: 'currency', currency }).format(minor/100);

export function FinancialDashboard() {
  const [from, setFrom] = useState(thirtyDaysAgo);
  const [to, setTo] = useState(today);
  const valid = from <= to;
  const metrics = useQuery({
    queryKey: ['platform-financial-overview', from, to],
    queryFn: () => request<FinancialOverview>(`/billing/platform/financial-overview?from=${from}&to=${to}`),
    enabled: valid,
  });
  return <section className="console-panel" aria-label="Resumen financiero">
    <div className="console-panel__header"><div><span className="console-overline">FACTURACIÓN</span><h2>Resumen financiero</h2><p>Ingresos registrados, facturas y suscripciones de la plataforma.</p></div></div>
    <div className="financial-filters">
      <label>Desde<input className="se-input" type="date" value={from} max={to} onChange={e => setFrom(e.target.value)}/></label>
      <label>Hasta<input className="se-input" type="date" value={to} min={from} onChange={e => setTo(e.target.value)}/></label>
      <button className="console-link" type="button" onClick={() => { setFrom(thirtyDaysAgo()); setTo(today()); }}>Últimos 30 días</button>
    </div>
    {!valid && <Alert>La fecha inicial no puede superar la final.</Alert>}
    {metrics.isLoading && <p>Calculando indicadores…</p>}
    {metrics.error && <Alert>{metrics.error.message}</Alert>}
    {metrics.data && <>
      {metrics.data.currencies.length === 0 && <p>No hay movimientos de facturación en el período seleccionado.</p>}
      {metrics.data.currencies.map(item => <div key={item.currency} className="financial-currency">
        <h3>{item.currency}</h3><div className="console-kpis console-kpis--compact">
          <div className="console-kpi"><span>Ingresos cobrados</span><strong>{money(item.collectedMinor,item.currency)}</strong></div>
          <div className="console-kpi"><span>Facturado</span><strong>{money(item.invoicedMinor,item.currency)}</strong></div>
          <div className="console-kpi"><span>Saldo pendiente</span><strong>{money(item.outstandingMinor,item.currency)}</strong><small>{item.openInvoices} facturas abiertas</small></div>
          <div className="console-kpi"><span>Saldo vencido</span><strong>{money(item.overdueMinor,item.currency)}</strong><small>{item.overdueInvoices} facturas vencidas</small></div>
        </div></div>)}
      <h3>Estado de suscripciones <small>(actual, independiente del rango)</small></h3>
      <div className="console-kpis console-kpis--compact">
        {([['Activas',metrics.data.subscriptions.active],['En prueba',metrics.data.subscriptions.trialing],['Vencidas',metrics.data.subscriptions.pastDue],['Canceladas',metrics.data.subscriptions.canceled],['Expiradas',metrics.data.subscriptions.expired],['Renuevan en 7 días',metrics.data.subscriptions.upcomingRenewals]] as const).map(([label,value]) => <div className="console-kpi" key={label}><span>{label}</span><strong>{value}</strong></div>)}
      </div>
    </>}
  </section>;
}
