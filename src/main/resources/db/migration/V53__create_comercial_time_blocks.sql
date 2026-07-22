CREATE TABLE comercial_time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id UUID NULL,
    room_id BIGINT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    reason TEXT,
    block_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_time_block_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_time_block_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE CASCADE,
    CONSTRAINT ck_comercial_time_block_range CHECK (end_date_time > start_date_time)
);

CREATE INDEX idx_comercial_time_block_studio ON comercial_time_blocks(studio_id);
CREATE INDEX idx_comercial_time_block_instructor ON comercial_time_blocks(instructor_id);
CREATE INDEX idx_comercial_time_block_dates ON comercial_time_blocks(start_date_time, end_date_time);
