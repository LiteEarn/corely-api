DROP TABLE IF EXISTS attendances CASCADE;

CREATE TABLE attendances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_session_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_class_session FOREIGN KEY (class_session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_session_enrollment UNIQUE (class_session_id, enrollment_id)
);

CREATE INDEX idx_attendance_class_session_id ON attendances(class_session_id);
CREATE INDEX idx_attendance_enrollment_id ON attendances(enrollment_id);
CREATE INDEX idx_attendance_status ON attendances(status);
