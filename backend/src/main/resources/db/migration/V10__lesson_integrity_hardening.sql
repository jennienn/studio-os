-- Cross-aggregate lesson references are made composite so PostgreSQL verifies
-- the same semantic relationships that application commands validate.
ALTER TABLE lesson_booking_details
    ADD CONSTRAINT uq_lesson_detail_booking_cycle
        UNIQUE (studio_id, booking_id, enrollment_cycle_id),
    ADD CONSTRAINT uq_lesson_detail_attendance_context
        UNIQUE (studio_id, booking_id, enrollment_cycle_id, class_occurrence_id);

ALTER TABLE pass_entitlement_reservations
    ADD CONSTRAINT fk_reservation_booking_cycle
        FOREIGN KEY (studio_id, booking_id, enrollment_cycle_id)
        REFERENCES lesson_booking_details (studio_id, booking_id, enrollment_cycle_id);

ALTER TABLE class_schedules
    ADD CONSTRAINT uq_class_schedule_class
        UNIQUE (studio_id, id, class_id);

ALTER TABLE class_occurrences
    ADD CONSTRAINT fk_occurrence_schedule_class
        FOREIGN KEY (studio_id, class_schedule_id, class_id)
        REFERENCES class_schedules (studio_id, id, class_id);

ALTER TABLE bookings
    ADD CONSTRAINT uq_booking_customer_context
        UNIQUE (studio_id, id, customer_id);

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_booking_customer
        FOREIGN KEY (studio_id, booking_id, customer_id)
        REFERENCES bookings (studio_id, id, customer_id),
    ADD CONSTRAINT fk_attendance_lesson_context
        FOREIGN KEY (studio_id, booking_id, enrollment_cycle_id, class_occurrence_id)
        REFERENCES lesson_booking_details (studio_id, booking_id, enrollment_cycle_id, class_occurrence_id);

ALTER TABLE enrollment_cycles
    ADD CONSTRAINT ck_time_cycle_resolved_period
        CHECK (product_type_snapshot <> 'TIME_BASED'
            OR (billing_period_snapshot IS NULL
                OR (billing_period_snapshot = 'MONTH'
                    AND validity_days_snapshot BETWEEN 28 AND 31)));
