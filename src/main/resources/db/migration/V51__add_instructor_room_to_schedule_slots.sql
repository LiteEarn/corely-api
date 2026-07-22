ALTER TABLE comercial_schedule_slots ADD COLUMN instructor_id UUID NULL REFERENCES instructors(id);
ALTER TABLE comercial_schedule_slots ADD COLUMN room_id BIGINT NULL;
CREATE INDEX idx_comercial_schedule_slot_instructor ON comercial_schedule_slots(instructor_id);
