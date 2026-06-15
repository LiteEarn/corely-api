CREATE TABLE class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    scheduled_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_students INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_session_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_session_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE CASCADE,
    CONSTRAINT chk_class_session_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_class_session_max_students CHECK (max_students > 0),
    CONSTRAINT chk_class_session_time CHECK (end_time > start_time)
);

CREATE INDEX idx_class_session_studio_id ON class_sessions(studio_id);
CREATE INDEX idx_class_session_instructor_id ON class_sessions(instructor_id);
CREATE INDEX idx_class_session_scheduled_date ON class_sessions(scheduled_date);
CREATE INDEX idx_class_session_status ON class_sessions(status);
