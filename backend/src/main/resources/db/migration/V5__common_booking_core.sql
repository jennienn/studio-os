ALTER TABLE staff ADD CONSTRAINT uq_staff_tenant_id UNIQUE(studio_id,id);
CREATE TABLE bookings (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), customer_id UUID NOT NULL, staff_id UUID,
 booking_kind VARCHAR(32) NOT NULL CHECK(booking_kind IN ('LESSON_PRIVATE','LESSON_GROUP','BEAUTY_SERVICE')),
 manual_entry BOOLEAN NOT NULL,
 start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW')),
 note VARCHAR(2000), source VARCHAR(32) NOT NULL CHECK(source IN ('OPERATOR','CUSTOMER_PORTAL')),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_booking_customer FOREIGN KEY(studio_id,customer_id) REFERENCES customers(studio_id,id),
 CONSTRAINT fk_booking_staff FOREIGN KEY(studio_id,staff_id) REFERENCES staff(studio_id,id),
 CONSTRAINT ck_booking_interval CHECK(start_at<end_at),
 CONSTRAINT ck_manual_one_to_one CHECK(NOT manual_entry OR (staff_id IS NOT NULL AND booking_kind<>'LESSON_GROUP' AND source='OPERATOR'))
);
CREATE INDEX ix_booking_range ON bookings(studio_id,start_at,id);
CREATE INDEX ix_booking_staff_occupancy ON bookings(studio_id,staff_id,start_at,end_at)
 WHERE status IN ('PENDING','CONFIRMED') AND booking_kind IN ('LESSON_PRIVATE','BEAUTY_SERVICE');
CREATE INDEX ix_booking_customer ON bookings(studio_id,customer_id,status);
CREATE TABLE booking_blocks (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id),
 scope_type VARCHAR(16) NOT NULL CHECK(scope_type IN ('STUDIO','STAFF')), staff_id UUID,
 start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL, reason VARCHAR(2000), created_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT fk_block_staff FOREIGN KEY(studio_id,staff_id) REFERENCES staff(studio_id,id),
 CONSTRAINT ck_block_scope CHECK ((scope_type='STUDIO' AND staff_id IS NULL) OR (scope_type='STAFF' AND staff_id IS NOT NULL)),
 CONSTRAINT ck_block_interval CHECK(start_at<end_at)
);
CREATE INDEX ix_block_range ON booking_blocks(studio_id,start_at,end_at);
-- All occupancy mutations acquire the Studio row lock and recheck under READ COMMITTED.
-- No staff-exclusive constraint is imposed on future GROUP capacity records.
