CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollment_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_enrollment_student_class_group UNIQUE (student_id, class_group_id)
);

CREATE INDEX idx_enrollment_studio_id ON enrollments(studio_id);
CREATE INDEX idx_enrollment_student_id ON enrollments(student_id);
CREATE INDEX idx_enrollment_class_group_id ON enrollments(class_group_id);
CREATE INDEX idx_enrollment_active ON enrollments(active);
CREATE INDEX idx_enrollment_enrollment_date ON enrollments(enrollment_date);
