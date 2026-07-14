CREATE TABLE comercial_schedule_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_schedule_slot_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_schedule_slot_schedule FOREIGN KEY (schedule_id) REFERENCES comercial_schedules(id) ON DELETE CASCADE,
    CONSTRAINT ck_comercial_schedule_slot_time CHECK (end_time > start_time),
    CONSTRAINT ck_comercial_schedule_slot_capacity CHECK (capacity > 0)
);

CREATE INDEX idx_comercial_schedule_slot_schedule ON comercial_schedule_slots(schedule_id);
CREATE INDEX idx_comercial_schedule_slot_schedule_day ON comercial_schedule_slots(schedule_id, day_of_week);
