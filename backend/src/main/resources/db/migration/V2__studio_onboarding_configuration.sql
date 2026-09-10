-- Replace only the Phase 2 pre-onboarding state constraint; retain slug uniqueness.
ALTER TABLE studios DROP CONSTRAINT studios_check;
ALTER TABLE studios ADD COLUMN configuration_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE studios ADD CONSTRAINT ck_studio_category_state CHECK (
 (status = 'PRE_ONBOARDING' AND business_category IS NULL AND business_type IS NULL)
 OR (status = 'ACTIVE' AND business_category IS NOT NULL AND business_type IS NOT NULL AND
   ((business_category = 'LESSON' AND business_type IN ('DANCE','PILATES','YOGA','PT','POLE','VOCAL_MUSIC','OTHER_LESSON'))
    OR (business_category = 'BEAUTY' AND business_type IN ('NAIL','EYELASH','HAIR_EXTENSION','WAXING','HAIR','OTHER_BEAUTY'))))
);
CREATE TABLE studio_capabilities (
 id UUID PRIMARY KEY,
 studio_id UUID NOT NULL REFERENCES studios(id),
 capability VARCHAR(32) NOT NULL CHECK (capability IN
   ('PRIVATE_LESSON','GROUP_CLASS','ATTENDANCE','CUSTOMER_BOOKING','PASS_MANAGEMENT','DEPOSIT','REVISIT')),
 enabled BOOLEAN NOT NULL,
 CONSTRAINT uq_studio_capability UNIQUE(studio_id,capability)
);
CREATE TABLE business_hours (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id),
 weekday INTEGER NOT NULL CHECK (weekday BETWEEN 1 AND 7),
 open_time TIME, close_time TIME, closed BOOLEAN NOT NULL,
 CONSTRAINT uq_studio_weekday UNIQUE(studio_id,weekday),
 CONSTRAINT ck_hours_interval CHECK (closed OR
   (open_time IS NOT NULL AND close_time IS NOT NULL AND open_time < close_time))
);
CREATE TABLE booking_policies (
 studio_id UUID PRIMARY KEY REFERENCES studios(id),
 slot_interval_minutes INTEGER NOT NULL CHECK (slot_interval_minutes BETWEEN 1 AND 1440),
 booking_window_days INTEGER NOT NULL CHECK (booking_window_days > 0),
 cancellation_cutoff_hours INTEGER NOT NULL CHECK (cancellation_cutoff_hours >= 0),
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE lesson_policies (
 studio_id UUID PRIMARY KEY REFERENCES studios(id),
 low_balance_threshold INTEGER NOT NULL CHECK (low_balance_threshold >= 0),
 expiry_alert_days INTEGER NOT NULL CHECK (expiry_alert_days >= 0),
 restore_on_timely_cancellation BOOLEAN NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE beauty_policies (
 studio_id UUID PRIMARY KEY REFERENCES studios(id),
 deposit_enabled BOOLEAN NOT NULL, no_show_enabled BOOLEAN NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
