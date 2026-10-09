CREATE TABLE customers (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL REFERENCES tenants(id),
 name VARCHAR(180) NOT NULL,
 legal_name VARCHAR(220),
 tax_id VARCHAR(50),
 email VARCHAR(255),
 phone VARCHAR(50),
 notes TEXT,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT uq_customers_tenant_id UNIQUE(tenant_id,id)
);
CREATE INDEX idx_customers_tenant_name ON customers(tenant_id,name);
CREATE TABLE service_locations (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL REFERENCES tenants(id),
 customer_id UUID NOT NULL,
 name VARCHAR(180) NOT NULL,
 address_line1 VARCHAR(255) NOT NULL,
 address_line2 VARCHAR(255),
 city VARCHAR(120) NOT NULL,
 state VARCHAR(120),
 postal_code VARCHAR(30),
 country VARCHAR(100) NOT NULL,
 contact_name VARCHAR(160),
 contact_phone VARCHAR(50),
 notes TEXT,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT fk_location_customer_tenant FOREIGN KEY (tenant_id,customer_id) REFERENCES customers(tenant_id,id)
);
CREATE INDEX idx_locations_tenant_customer ON service_locations(tenant_id,customer_id);
