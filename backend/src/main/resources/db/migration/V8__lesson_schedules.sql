CREATE TABLE class_schedules (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), class_id UUID NOT NULL,
 weekday INTEGER NOT NULL CHECK(weekday BETWEEN 1 AND 7), start_time TIME NOT NULL,
 duration_minutes INTEGER NOT NULL CHECK(duration_minutes>0 AND duration_minutes<1440), active BOOLEAN NOT NULL,
 UNIQUE(studio_id,id), FOREIGN KEY(studio_id,class_id) REFERENCES classes(studio_id,id)
);
CREATE TABLE class_occurrences (
 id UUID PRIMARY KEY, studio_id UUID NOT NULL REFERENCES studios(id), class_id UUID NOT NULL, class_schedule_id UUID,
 occurrence_date DATE NOT NULL, start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL,
 instructor_staff_id UUID, capacity_snapshot INTEGER NOT NULL CHECK(capacity_snapshot>0),
 status VARCHAR(16) NOT NULL CHECK(status IN ('SCHEDULED','CANCELLED','COMPLETED')),
 UNIQUE(studio_id,id), UNIQUE(studio_id,class_schedule_id,occurrence_date),
 FOREIGN KEY(studio_id,class_id) REFERENCES classes(studio_id,id),
 FOREIGN KEY(studio_id,class_schedule_id) REFERENCES class_schedules(studio_id,id),
 FOREIGN KEY(studio_id,instructor_staff_id) REFERENCES staff(studio_id,id), CHECK(start_at<end_at)
);
CREATE INDEX ix_occurrence_range ON class_occurrences(studio_id,start_at);
CREATE INDEX ix_occurrence_class ON class_occurrences(studio_id,class_id,start_at);
CREATE INDEX ix_occurrence_instructor ON class_occurrences(studio_id,instructor_staff_id,start_at,end_at) WHERE status='SCHEDULED';
ALTER TABLE lesson_booking_details ADD CONSTRAINT fk_detail_occurrence FOREIGN KEY(studio_id,class_occurrence_id) REFERENCES class_occurrences(studio_id,id);
CREATE INDEX ix_detail_occurrence ON lesson_booking_details(studio_id,class_occurrence_id);
