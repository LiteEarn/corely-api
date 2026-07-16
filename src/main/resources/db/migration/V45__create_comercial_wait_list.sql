CREATE TABLE comercial_wait_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    class_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    position INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_wait_list_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_wait_list_class_session FOREIGN KEY (class_session_id) REFERENCES comercial_class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_wait_list_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_wait_list_session_student UNIQUE (class_session_id, student_id, active)
);

CREATE INDEX idx_comercial_wait_list_class_session ON comercial_wait_list(class_session_id);
CREATE INDEX idx_comercial_wait_list_student ON comercial_wait_list(student_id);
CREATE INDEX idx_comercial_wait_list_status ON comercial_wait_list(status);
CREATE INDEX idx_comercial_wait_list_position ON comercial_wait_list(class_session_id, position);
