CREATE TABLE service_evidence (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, work_order_id UUID NOT NULL, uploaded_by UUID NOT NULL REFERENCES platform_users(id),
 kind VARCHAR(30) NOT NULL, object_key VARCHAR(500) NOT NULL, content_type VARCHAR(120) NOT NULL, byte_size BIGINT NOT NULL,
 sha256 VARCHAR(64) NOT NULL, notes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_evidence_order FOREIGN KEY (tenant_id,work_order_id) REFERENCES work_orders(tenant_id,id),
 CONSTRAINT ck_evidence_size CHECK (byte_size > 0 AND byte_size <= 100000000)
);
CREATE INDEX idx_evidence_order ON service_evidence(tenant_id,work_order_id,created_at DESC);
