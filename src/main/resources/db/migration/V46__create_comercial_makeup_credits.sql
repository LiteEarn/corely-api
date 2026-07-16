CREATE TABLE comercial_makeup_credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    original_attendance_id UUID NOT NULL,
    original_class_session_id UUID NOT NULL,
    makeup_booking_id UUID,
    expiration_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    reason VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_makeup_credits_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_original_attendance FOREIGN KEY (original_attendance_id) REFERENCES comercial_attendances(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_original_class_session FOREIGN KEY (original_class_session_id) REFERENCES comercial_class_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_makeup_credits_makeup_booking FOREIGN KEY (makeup_booking_id) REFERENCES comercial_bookings(id) ON DELETE SET NULL
);

CREATE INDEX idx_comercial_makeup_credits_student ON comercial_makeup_credits(student_id);
CREATE INDEX idx_comercial_makeup_credits_status ON comercial_makeup_credits(status);
CREATE INDEX idx_comercial_makeup_credits_expiration_date ON comercial_makeup_credits(expiration_date);
