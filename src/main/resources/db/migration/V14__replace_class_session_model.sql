DROP TABLE IF EXISTS class_sessions CASCADE;

CREATE TABLE class_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_group_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_session_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_class_session_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE CASCADE,
    CONSTRAINT uq_class_session_group_date UNIQUE (class_group_id, session_date)
);

CREATE INDEX idx_class_session_class_group_id ON class_sessions(class_group_id);
CREATE INDEX idx_class_session_session_date ON class_sessions(session_date);
CREATE INDEX idx_class_session_status ON class_sessions(status);
