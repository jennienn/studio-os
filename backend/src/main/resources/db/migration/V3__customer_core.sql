CREATE TABLE customers (
 id UUID PRIMARY KEY,
 studio_id UUID NOT NULL REFERENCES studios(id),
 name VARCHAR(100) NOT NULL CHECK (length(btrim(name)) > 0),
 phone VARCHAR(32) NOT NULL,
 normalized_phone VARCHAR(11) NOT NULL CHECK (normalized_phone ~ '^0[1-9][0-9]{7,9}$'),
 memo VARCHAR(2000) NOT NULL DEFAULT '',
 status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 archived_at TIMESTAMPTZ,
 CONSTRAINT uq_customer_tenant_id UNIQUE(studio_id,id),
 CONSTRAINT uq_customer_identity UNIQUE(studio_id,normalized_phone,name),
 CONSTRAINT ck_customer_archive CHECK ((status='ACTIVE' AND archived_at IS NULL)
   OR (status='ARCHIVED' AND archived_at IS NOT NULL))
);
-- Identity uniqueness also supports tenant/normalized-phone lookups.
CREATE INDEX ix_customer_name ON customers(studio_id,name,id);
