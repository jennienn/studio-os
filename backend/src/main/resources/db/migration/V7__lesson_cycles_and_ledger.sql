CREATE TABLE enrollment_cycles (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), enrollment_id UUID NOT NULL, pass_product_id UUID,
 product_name_snapshot VARCHAR(100) NOT NULL, product_type_snapshot VARCHAR(16) NOT NULL,
 purchased_count INTEGER, validity_days_snapshot INTEGER CHECK(validity_days_snapshot>0), billing_period_snapshot VARCHAR(16),
 purchase_price_snapshot BIGINT NOT NULL CHECK(purchase_price_snapshot>0), deduction_trigger_snapshot VARCHAR(32), validity_start_rule_snapshot VARCHAR(16) NOT NULL,
 start_date DATE, valid_end_date DATE, payment_date DATE NOT NULL, next_due_date DATE,
 status VARCHAR(16) NOT NULL CHECK(status IN ('ACTIVE','SCHEDULED','COMPLETED','EXPIRED','CANCELLED')), created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(studio_id,id), FOREIGN KEY(studio_id,enrollment_id) REFERENCES enrollments(studio_id,id), FOREIGN KEY(studio_id,pass_product_id) REFERENCES pass_products(studio_id,id),
 CHECK(validity_start_rule_snapshot IN ('PURCHASE_DATE','FIRST_USE')),
 CHECK((product_type_snapshot='COUNT_BASED' AND purchased_count IS NOT NULL AND purchased_count>0 AND deduction_trigger_snapshot IS NOT NULL AND deduction_trigger_snapshot IN ('BOOKING_CONFIRMED','LESSON_COMPLETED','ATTENDANCE_PRESENT') AND billing_period_snapshot IS NULL)
 OR (product_type_snapshot='TIME_BASED' AND purchased_count IS NULL AND deduction_trigger_snapshot IS NULL AND validity_start_rule_snapshot='PURCHASE_DATE' AND validity_days_snapshot IS NOT NULL AND status<>'SCHEDULED')),
 CHECK(valid_end_date IS NULL OR (start_date IS NOT NULL AND valid_end_date>=start_date))
);
CREATE UNIQUE INDEX uq_enrollment_active_cycle ON enrollment_cycles(studio_id,enrollment_id) WHERE status='ACTIVE';
CREATE UNIQUE INDEX uq_enrollment_scheduled_cycle ON enrollment_cycles(studio_id,enrollment_id) WHERE status='SCHEDULED';
CREATE INDEX ix_cycle_enrollment ON enrollment_cycles(studio_id,enrollment_id,created_at);
ALTER TABLE bookings ADD CONSTRAINT uq_booking_tenant_id UNIQUE(studio_id,id);
CREATE TABLE lesson_booking_details (
 booking_id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), enrollment_cycle_id UUID NOT NULL,
 class_occurrence_id UUID, lesson_mode VARCHAR(16) NOT NULL,
 FOREIGN KEY(studio_id,booking_id) REFERENCES bookings(studio_id,id), FOREIGN KEY(studio_id,enrollment_cycle_id) REFERENCES enrollment_cycles(studio_id,id),
 CHECK((lesson_mode='PRIVATE' AND class_occurrence_id IS NULL) OR (lesson_mode='GROUP' AND class_occurrence_id IS NOT NULL))
);
CREATE INDEX ix_detail_cycle ON lesson_booking_details(studio_id,enrollment_cycle_id);
CREATE TABLE pass_entitlement_reservations (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), enrollment_cycle_id UUID NOT NULL, booking_id UUID NOT NULL,
 status VARCHAR(16) NOT NULL CHECK(status IN ('ACTIVE','CONSUMED','RELEASED')), created_at TIMESTAMPTZ NOT NULL, resolved_at TIMESTAMPTZ,
 UNIQUE(studio_id,booking_id), FOREIGN KEY(studio_id,enrollment_cycle_id) REFERENCES enrollment_cycles(studio_id,id), FOREIGN KEY(studio_id,booking_id) REFERENCES bookings(studio_id,id),
 CHECK((status='ACTIVE' AND resolved_at IS NULL) OR (status<>'ACTIVE' AND resolved_at IS NOT NULL))
);
CREATE INDEX ix_reservation_cycle ON pass_entitlement_reservations(studio_id,enrollment_cycle_id,status);
CREATE TABLE pass_usage_ledger (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), enrollment_cycle_id UUID NOT NULL,
 event_type VARCHAR(32) NOT NULL CHECK(event_type IN ('PURCHASE','BOOKING_DEDUCTION','ATTENDANCE','LESSON_COMPLETED','CANCEL_RESTORE','MANUAL_ADJUSTMENT')),
 amount INTEGER NOT NULL CHECK(amount<>0), reason VARCHAR(2000), reference_type VARCHAR(32) NOT NULL, reference_id UUID NOT NULL,
 created_by_user_id UUID REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(studio_id,enrollment_cycle_id) REFERENCES enrollment_cycles(studio_id,id),
 UNIQUE(studio_id,enrollment_cycle_id,event_type,reference_type,reference_id),
 CHECK((event_type='PURCHASE' AND amount>0) OR (event_type IN ('BOOKING_DEDUCTION','ATTENDANCE','LESSON_COMPLETED') AND amount=-1)
 OR (event_type='CANCEL_RESTORE' AND amount=1) OR (event_type='MANUAL_ADJUSTMENT' AND reason IS NOT NULL AND length(btrim(reason))>0 AND created_by_user_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_cycle_purchase ON pass_usage_ledger(studio_id,enrollment_cycle_id) WHERE event_type='PURCHASE';
CREATE INDEX ix_ledger_history ON pass_usage_ledger(studio_id,enrollment_cycle_id,created_at,id);
CREATE TABLE lesson_cycle_payments (
 studio_id UUID NOT NULL REFERENCES studios(id), enrollment_cycle_id UUID NOT NULL, payment_id UUID NOT NULL,
 PRIMARY KEY(studio_id,enrollment_cycle_id), UNIQUE(studio_id,payment_id),
 FOREIGN KEY(studio_id,enrollment_cycle_id) REFERENCES enrollment_cycles(studio_id,id),
 FOREIGN KEY(studio_id,payment_id) REFERENCES payments(studio_id,id)
);
