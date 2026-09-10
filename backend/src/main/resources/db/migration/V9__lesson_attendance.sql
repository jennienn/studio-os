CREATE TABLE attendance (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), class_occurrence_id UUID NOT NULL,
 customer_id UUID NOT NULL, enrollment_cycle_id UUID NOT NULL, booking_id UUID NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('PRESENT','ABSENT','CANCELLED')),
 recorded_at TIMESTAMPTZ NOT NULL, recorded_by_user_id UUID NOT NULL REFERENCES users(id),
 UNIQUE(studio_id,class_occurrence_id,customer_id), UNIQUE(studio_id,booking_id),
 FOREIGN KEY(studio_id,class_occurrence_id) REFERENCES class_occurrences(studio_id,id),
 FOREIGN KEY(studio_id,customer_id) REFERENCES customers(studio_id,id),
 FOREIGN KEY(studio_id,enrollment_cycle_id) REFERENCES enrollment_cycles(studio_id,id),
 FOREIGN KEY(studio_id,booking_id) REFERENCES bookings(studio_id,id)
);
CREATE INDEX ix_attendance_occurrence ON attendance(studio_id,class_occurrence_id);
