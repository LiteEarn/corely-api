CREATE TABLE comercial_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    class_session_id UUID NOT NULL,
    student_id UUID NOT NULL,
    booking_date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_booking_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_booking_class_session FOREIGN KEY (class_session_id) REFERENCES comercial_class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_booking_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_booking_class_session_student UNIQUE (class_session_id, student_id)
);

CREATE INDEX idx_comercial_booking_class_session ON comercial_bookings(class_session_id);
CREATE INDEX idx_comercial_booking_student ON comercial_bookings(student_id);
CREATE INDEX idx_comercial_booking_status ON comercial_bookings(status);
