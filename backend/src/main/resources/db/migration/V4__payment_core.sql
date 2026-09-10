CREATE TABLE payments (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), customer_id UUID NOT NULL,
 amount BIGINT NOT NULL CHECK(amount>0), currency VARCHAR(3) NOT NULL CHECK(currency='KRW'),
 method VARCHAR(16) NOT NULL CHECK(method IN ('CARD','CASH','TRANSFER','OTHER')),
 status VARCHAR(16) NOT NULL CHECK(status IN ('PENDING','PAID','REFUNDED','CANCELLED')),
 reference_type VARCHAR(32) NOT NULL CHECK(reference_type IN ('LESSON_CYCLE','RENEWAL','BEAUTY_TREATMENT','DEPOSIT','OTHER')),
 reference_id UUID, paid_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL,
 created_by_user_id UUID NOT NULL REFERENCES users(id),
 CONSTRAINT uq_payment_tenant_id UNIQUE(studio_id,id),
 CONSTRAINT fk_payment_customer FOREIGN KEY(studio_id,customer_id) REFERENCES customers(studio_id,id),
 CONSTRAINT ck_payment_paid_at CHECK ((status IN ('PAID','REFUNDED') AND paid_at IS NOT NULL)
   OR (status IN ('PENDING','CANCELLED') AND paid_at IS NULL))
);
CREATE INDEX ix_payment_history ON payments(studio_id,created_at DESC,id);
CREATE INDEX ix_payment_customer ON payments(studio_id,customer_id,created_at DESC);
CREATE TABLE payment_refunds (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), payment_id UUID NOT NULL,
 amount BIGINT NOT NULL CHECK(amount>0), reason VARCHAR(2000) NOT NULL CHECK(length(btrim(reason))>0),
 refunded_at TIMESTAMPTZ NOT NULL, created_by_user_id UUID NOT NULL REFERENCES users(id),
 CONSTRAINT fk_refund_payment FOREIGN KEY(studio_id,payment_id) REFERENCES payments(studio_id,id),
 CONSTRAINT uq_full_refund UNIQUE(studio_id,payment_id)
);
CREATE TABLE idempotency_records (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id),
 actor_type VARCHAR(32) NOT NULL CHECK(actor_type IN ('OPERATOR_USER','CUSTOMER_PORTAL','SYSTEM')),
 actor_id VARCHAR(128) NOT NULL, operation VARCHAR(64) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 request_hash VARCHAR(64) NOT NULL, response_status INTEGER NOT NULL, response_body TEXT NOT NULL,
 resource_type VARCHAR(64), resource_id UUID, created_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ,
 CONSTRAINT uq_idempotency_namespace UNIQUE(studio_id,actor_type,actor_id,operation,idempotency_key)
);
-- Successful operator replay records are retained; no key expiry/reuse or cleanup is enabled.
