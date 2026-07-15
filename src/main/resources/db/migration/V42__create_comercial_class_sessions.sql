CREATE TABLE comercial_class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    schedule_slot_id UUID NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    booked_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_class_session_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_class_session_slot FOREIGN KEY (schedule_slot_id) REFERENCES comercial_schedule_slots(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_class_session_slot_date UNIQUE (schedule_slot_id, session_date),
    CONSTRAINT ck_comercial_class_session_time CHECK (end_time > start_time)
);

CREATE INDEX idx_comercial_class_session_slot ON comercial_class_sessions(schedule_slot_id);
CREATE INDEX idx_comercial_class_session_date ON comercial_class_sessions(session_date);
CREATE INDEX idx_comercial_class_session_status ON comercial_class_sessions(status);
