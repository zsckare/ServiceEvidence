CREATE TABLE platform_users (
 id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 full_name VARCHAR(160) NOT NULL, platform_admin BOOLEAN NOT NULL DEFAULT FALSE,
 enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE tenant_memberships (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id), user_id UUID NOT NULL REFERENCES platform_users(id),
 role VARCHAR(32) NOT NULL CHECK (role IN ('TENANT_ADMIN','SUPERVISOR','TECHNICIAN','CLIENT')),
 enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_tenant_user UNIQUE(tenant_id,user_id)
);
CREATE INDEX idx_membership_user ON tenant_memberships(user_id);
CREATE TABLE refresh_sessions (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES platform_users(id),
 token_hash VARCHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMPTZ NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user ON refresh_sessions(user_id);
CREATE TABLE security_audit (
 id UUID PRIMARY KEY, actor_user_id UUID REFERENCES platform_users(id),
 tenant_id UUID REFERENCES tenants(id), action VARCHAR(100) NOT NULL,
 target_id VARCHAR(100), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
