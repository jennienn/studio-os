CREATE TABLE users (
 id UUID PRIMARY KEY, email VARCHAR(254), name VARCHAR(100) NOT NULL,
 status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','DISABLED')),
 security_version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE auth_accounts (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id),
 provider VARCHAR(16) NOT NULL CHECK (provider IN ('EMAIL','GOOGLE','KAKAO')),
 provider_user_id VARCHAR(254) NOT NULL, password_hash VARCHAR(100),
 verified_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_auth_identity UNIQUE(provider, provider_user_id),
 CHECK ((provider = 'EMAIL' AND password_hash IS NOT NULL) OR
        (provider <> 'EMAIL' AND password_hash IS NULL)),
 CHECK (provider <> 'EMAIL' OR provider_user_id = lower(btrim(provider_user_id)))
);
CREATE INDEX ix_auth_user ON auth_accounts(user_id);
CREATE TABLE email_verification_tokens (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id),
 token_hash VARCHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_verification_user ON email_verification_tokens(user_id);
CREATE TABLE password_reset_tokens (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id),
 token_hash VARCHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_reset_user ON password_reset_tokens(user_id);
CREATE TABLE studios (
 id UUID PRIMARY KEY, name VARCHAR(100) NOT NULL,
 slug VARCHAR(63) NOT NULL CONSTRAINT uq_studio_slug UNIQUE,
 business_category VARCHAR(16), business_type VARCHAR(32),
 timezone VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$' AND length(slug) BETWEEN 3 AND 63),
 -- Phase 2 explicitly has no category. Phase 3 will migrate this constraint.
 CHECK (status = 'PRE_ONBOARDING' AND business_category IS NULL AND business_type IS NULL)
);
CREATE TABLE studio_memberships (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id),
 user_id UUID NOT NULL REFERENCES users(id),
 role VARCHAR(16) NOT NULL CHECK (role IN ('OWNER','MANAGER','STAFF')),
 status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
 created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_membership UNIQUE(studio_id,user_id)
);
CREATE INDEX ix_membership_user_status ON studio_memberships(user_id,status);
CREATE TABLE staff (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id),
 user_id UUID REFERENCES users(id), name VARCHAR(100) NOT NULL, phone VARCHAR(32),
 active BOOLEAN NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_staff_membership FOREIGN KEY(studio_id,user_id)
   REFERENCES studio_memberships(studio_id,user_id),
 CONSTRAINT uq_staff_user UNIQUE(studio_id,user_id)
);
