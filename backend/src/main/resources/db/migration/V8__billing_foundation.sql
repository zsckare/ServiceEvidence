-- EN: Billing is recorded internally; no payment gateway is charged.
-- ES: La facturación es interna; no se realiza ningún cargo bancario.
ALTER TABLE subscription_plans
    ADD COLUMN monthly_price_minor BIGINT NOT NULL DEFAULT 0 CHECK (monthly_price_minor >= 0),
    ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'MXN';

ALTER TABLE tenant_subscriptions
    ADD COLUMN billing_status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN billing_period VARCHAR(12) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN current_period_start TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN current_period_end TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 month'),
    ADD COLUMN trial_ends_at TIMESTAMPTZ,
    ADD COLUMN cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT tenant_subscription_billing_status_ck CHECK (billing_status IN ('TRIALING','ACTIVE','PAST_DUE','CANCELED')),
    ADD CONSTRAINT tenant_subscription_period_ck CHECK (billing_period IN ('MONTHLY','YEARLY')),
    ADD CONSTRAINT tenant_subscription_dates_ck CHECK (current_period_end > current_period_start);

CREATE TABLE billing_invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    invoice_number VARCHAR(48) NOT NULL UNIQUE,
    plan_code VARCHAR(32) NOT NULL REFERENCES subscription_plans(code),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','PAID','VOID')),
    currency CHAR(3) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    CONSTRAINT billing_invoice_payment_dates_ck CHECK (paid_at IS NULL OR status = 'PAID')
);
CREATE INDEX billing_invoices_tenant_issued_idx ON billing_invoices(tenant_id, issued_at DESC);

CREATE TABLE billing_payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES billing_invoices(id),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    currency CHAR(3) NOT NULL,
    method VARCHAR(24) NOT NULL CHECK (method IN ('BANK_TRANSFER','CASH','OTHER')),
    external_reference VARCHAR(120),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by UUID REFERENCES platform_users(id)
);
CREATE INDEX billing_payments_invoice_idx ON billing_payments(invoice_id);

CREATE TABLE billing_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    event_type VARCHAR(60) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX billing_events_tenant_created_idx ON billing_events(tenant_id, created_at DESC);
