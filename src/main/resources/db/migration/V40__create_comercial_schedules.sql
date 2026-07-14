CREATE TABLE comercial_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_schedule_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_schedule_studio_name UNIQUE (studio_id, name)
);

CREATE INDEX idx_comercial_schedule_studio_id ON comercial_schedules(studio_id);
