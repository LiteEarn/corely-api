ALTER TABLE class_sessions
    ADD COLUMN cancel_reason VARCHAR(30),
    ADD COLUMN cancel_description VARCHAR(500),
    ADD COLUMN cancelled_by UUID REFERENCES users(id),
    ADD COLUMN cancelled_at TIMESTAMP;

CREATE TABLE makeup_eligibility (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ELIGIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_makeup_eligibility_session FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_eligibility_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_eligibility_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_eligibility_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE CASCADE
);

CREATE INDEX idx_makeup_eligibility_session_id ON makeup_eligibility(session_id);
CREATE INDEX idx_makeup_eligibility_student_id ON makeup_eligibility(student_id);
CREATE INDEX idx_makeup_eligibility_status ON makeup_eligibility(status);
