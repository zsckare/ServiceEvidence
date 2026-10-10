-- EN: Partial payments, idempotency, and subscription renewal audit.
-- ES: Pagos parciales, idempotencia y auditoría de renovaciones.
ALTER TABLE billing_invoices DROP CONSTRAINT IF EXISTS billing_invoices_status_check;
ALTER TABLE billing_invoices ADD CONSTRAINT billing_invoice_status_v9_ck CHECK (status IN ('OPEN','PARTIALLY_PAID','PAID','VOID'));
ALTER TABLE billing_invoices ADD COLUMN IF NOT EXISTS paid_amount_minor BIGINT NOT NULL DEFAULT 0 CHECK (paid_amount_minor >= 0);
ALTER TABLE billing_invoices ADD CONSTRAINT billing_invoice_balance_ck CHECK (paid_amount_minor <= amount_minor);
ALTER TABLE billing_payments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);
CREATE UNIQUE INDEX billing_payment_idempotency_idx ON billing_payments(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX billing_invoices_due_idx ON billing_invoices(due_at) WHERE status IN ('OPEN','PARTIALLY_PAID');
CREATE TABLE billing_renewal_runs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    period_end TIMESTAMPTZ NOT NULL,
    result VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, period_end)
);
CREATE INDEX billing_renewal_runs_tenant_idx ON billing_renewal_runs(tenant_id, created_at DESC);
