CREATE TABLE tenant_settings (
 tenant_id UUID PRIMARY KEY REFERENCES tenants(id),
 display_name VARCHAR(160) NOT NULL DEFAULT '',
 support_email VARCHAR(200) NOT NULL DEFAULT '',
 timezone VARCHAR(80) NOT NULL DEFAULT 'UTC'
);
CREATE TABLE service_notifications (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id),
 user_id UUID NOT NULL REFERENCES platform_users(id),
 title VARCHAR(255) NOT NULL, message TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), read_at TIMESTAMPTZ
);
CREATE INDEX idx_service_notifications_user ON service_notifications(tenant_id,user_id,created_at DESC);
