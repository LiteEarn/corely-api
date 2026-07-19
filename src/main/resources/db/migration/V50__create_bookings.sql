CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id UUID NOT NULL,
    student_id UUID NOT NULL,
    room_id BIGINT,
    class_type VARCHAR(100) NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    capacity INTEGER,
    make_up_class BOOLEAN NOT NULL DEFAULT FALSE,
    original_booking_id UUID,
    cancellation_reason VARCHAR(20),
    cancellation_notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_instructor FOREIGN KEY (instructor_id) REFERENCES instructors(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_original FOREIGN KEY (original_booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
    CONSTRAINT chk_booking_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_booking_cancel_reason CHECK (cancellation_reason IN ('STUDENT', 'STUDIO', 'INSTRUCTOR', 'WEATHER', 'OTHER')),
    CONSTRAINT chk_booking_time CHECK (end_date_time > start_date_time)
);

CREATE INDEX idx_booking_studio_id ON bookings(studio_id);
CREATE INDEX idx_booking_instructor_id ON bookings(instructor_id);
CREATE INDEX idx_booking_student_id ON bookings(student_id);
CREATE INDEX idx_booking_room_id ON bookings(room_id);
CREATE INDEX idx_booking_start_date_time ON bookings(start_date_time);
CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_booking_active ON bookings(active);

CREATE TABLE time_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    instructor_id BIGINT,
    room_id BIGINT,
    block_type VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_block_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT chk_block_type CHECK (block_type IN ('INSTRUCTOR_VACATION', 'ROOM_MAINTENANCE', 'HOLIDAY', 'ADMINISTRATIVE')),
    CONSTRAINT chk_block_time CHECK (end_date > start_date)
);

CREATE INDEX idx_time_block_studio_id ON time_blocks(studio_id);
CREATE INDEX idx_time_block_instructor_id ON time_blocks(instructor_id);
CREATE INDEX idx_time_block_room_id ON time_blocks(room_id);
CREATE INDEX idx_time_block_start_date ON time_blocks(start_date);
CREATE INDEX idx_time_block_end_date ON time_blocks(end_date);
