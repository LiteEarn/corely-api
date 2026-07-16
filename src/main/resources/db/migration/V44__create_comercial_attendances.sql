CREATE TABLE comercial_attendances (
    id UUID NOT NULL,
    studio_id UUID NOT NULL,
    booking_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500),
    checked_in_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_comercial_attendances PRIMARY KEY (id),
    CONSTRAINT fk_comercial_attendances_studio FOREIGN KEY (studio_id) REFERENCES studios(id),
    CONSTRAINT fk_comercial_attendances_booking FOREIGN KEY (booking_id) REFERENCES comercial_bookings(id),
    CONSTRAINT uq_comercial_attendances_booking UNIQUE (booking_id)
);

CREATE INDEX idx_comercial_attendances_booking_id ON comercial_attendances(booking_id);
CREATE INDEX idx_comercial_attendances_student_id ON comercial_attendances(studio_id);
