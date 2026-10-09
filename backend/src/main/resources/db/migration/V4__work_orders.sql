CREATE TABLE work_orders (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL REFERENCES tenants(id), customer_id UUID NOT NULL, location_id UUID NOT NULL,
 title VARCHAR(180) NOT NULL, description TEXT, priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
 scheduled_at TIMESTAMPTZ, started_at TIMESTAMPTZ, completed_at TIMESTAMPTZ, created_by UUID NOT NULL REFERENCES platform_users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_order_customer FOREIGN KEY (tenant_id,customer_id) REFERENCES customers(tenant_id,id),
 CONSTRAINT ck_order_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
 CONSTRAINT ck_order_status CHECK (status IN ('DRAFT','SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
 CONSTRAINT uq_work_orders_tenant_id UNIQUE(tenant_id,id)
);
CREATE INDEX idx_orders_tenant_status ON work_orders(tenant_id,status,created_at DESC);
CREATE TABLE work_order_assignments (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, work_order_id UUID NOT NULL, technician_id UUID NOT NULL REFERENCES platform_users(id), is_primary BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT fk_assignment_order FOREIGN KEY (tenant_id,work_order_id) REFERENCES work_orders(tenant_id,id),
 CONSTRAINT uq_assignment UNIQUE(tenant_id,work_order_id,technician_id)
);
CREATE UNIQUE INDEX uq_primary_assignment ON work_order_assignments(tenant_id,work_order_id) WHERE is_primary;
CREATE INDEX idx_assignment_technician ON work_order_assignments(tenant_id,technician_id);
CREATE TABLE work_order_events (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, work_order_id UUID NOT NULL, actor_id UUID NOT NULL REFERENCES platform_users(id),
 action VARCHAR(60) NOT NULL, details TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_event_order FOREIGN KEY (tenant_id,work_order_id) REFERENCES work_orders(tenant_id,id)
);
CREATE INDEX idx_order_events ON work_order_events(tenant_id,work_order_id,created_at);
