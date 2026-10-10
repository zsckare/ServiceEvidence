-- EN: Subscription catalog and one active plan per tenant.
-- ES: Catálogo de planes y una suscripción vigente por empresa.
CREATE TABLE subscription_plans (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    max_users INTEGER NOT NULL CHECK (max_users > 0),
    max_work_orders_per_month INTEGER NOT NULL CHECK (max_work_orders_per_month > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO subscription_plans (code, name, max_users, max_work_orders_per_month) VALUES
    ('STARTER', 'Starter', 5, 100),
    ('GROWTH', 'Growth', 25, 1000),
    ('BUSINESS', 'Business', 100, 10000);

CREATE TABLE tenant_subscriptions (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
    plan_code VARCHAR(32) NOT NULL REFERENCES subscription_plans(code),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Existing tenants are grandfathered into Business for a safe rollout.
INSERT INTO tenant_subscriptions (tenant_id, plan_code)
SELECT id, 'BUSINESS' FROM tenants;
