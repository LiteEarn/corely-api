CREATE TABLE attendances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    class_group_id UUID NOT NULL,
    attendance_date DATE NOT NULL,
    present BOOLEAN NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_class_group FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_student_class_group_date UNIQUE (student_id, class_group_id, attendance_date)
);

CREATE INDEX idx_attendance_studio_id ON attendances(studio_id);
CREATE INDEX idx_attendance_student_id ON attendances(student_id);
CREATE INDEX idx_attendance_class_group_id ON attendances(class_group_id);
CREATE INDEX idx_attendance_attendance_date ON attendances(attendance_date);
CREATE INDEX idx_attendance_present ON attendances(present);
