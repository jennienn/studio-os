CREATE TABLE pass_products (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), name VARCHAR(100) NOT NULL CHECK(length(btrim(name))>0),
 product_type VARCHAR(16) NOT NULL CHECK(product_type IN ('COUNT_BASED','TIME_BASED')),
 total_count INTEGER, validity_days INTEGER CHECK(validity_days>0), validity_start_rule VARCHAR(16) NOT NULL CHECK(validity_start_rule IN ('PURCHASE_DATE','FIRST_USE')),
 price BIGINT NOT NULL CHECK(price>0), deduction_trigger VARCHAR(32), billing_period VARCHAR(16), active BOOLEAN NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 UNIQUE(studio_id,id),
 CHECK((product_type='COUNT_BASED' AND total_count IS NOT NULL AND total_count>0 AND deduction_trigger IS NOT NULL AND deduction_trigger IN ('BOOKING_CONFIRMED','LESSON_COMPLETED','ATTENDANCE_PRESENT') AND billing_period IS NULL)
 OR (product_type='TIME_BASED' AND total_count IS NULL AND deduction_trigger IS NULL AND validity_start_rule='PURCHASE_DATE'
 AND ((validity_days IS NOT NULL AND billing_period IS NULL) OR (validity_days IS NULL AND billing_period IS NOT NULL AND billing_period='MONTH'))))
);
CREATE INDEX ix_pass_active ON pass_products(studio_id,active,name,id);
-- Class identity is required for the GROUP Enrollment FK; schedule operations arrive in Step C.
CREATE TABLE classes (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), name VARCHAR(100) NOT NULL CHECK(length(btrim(name))>0),
 capacity INTEGER NOT NULL CHECK(capacity>0), instructor_staff_id UUID, active BOOLEAN NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(studio_id,id), FOREIGN KEY(studio_id,instructor_staff_id) REFERENCES staff(studio_id,id)
);
CREATE INDEX ix_class_active ON classes(studio_id,active,name,id);
CREATE TABLE enrollments (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), customer_id UUID NOT NULL,
 kind VARCHAR(16) NOT NULL CHECK(kind IN ('PRIVATE','GROUP')), class_id UUID,
 status VARCHAR(16) NOT NULL CHECK(status IN ('ACTIVE','ENDED')), created_at TIMESTAMPTZ NOT NULL, ended_at TIMESTAMPTZ,
 UNIQUE(studio_id,id), FOREIGN KEY(studio_id,customer_id) REFERENCES customers(studio_id,id),
 FOREIGN KEY(studio_id,class_id) REFERENCES classes(studio_id,id),
 CHECK((kind='PRIVATE' AND class_id IS NULL) OR (kind='GROUP' AND class_id IS NOT NULL)),
 CHECK((status='ACTIVE' AND ended_at IS NULL) OR (status='ENDED' AND ended_at IS NOT NULL))
);
CREATE INDEX ix_enrollment_customer ON enrollments(studio_id,customer_id,status);
