CREATE TABLE makeup_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_id UUID NOT NULL,
    target_session_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason VARCHAR(500),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_makeup_request_attendance FOREIGN KEY (attendance_id) REFERENCES attendances(id) ON DELETE CASCADE,
    CONSTRAINT fk_makeup_request_target_session FOREIGN KEY (target_session_id) REFERENCES class_sessions(id) ON DELETE SET NULL,
    CONSTRAINT uq_makeup_request_attendance UNIQUE (attendance_id)
);

CREATE INDEX idx_makeup_request_attendance_id ON makeup_requests(attendance_id);
CREATE INDEX idx_makeup_request_target_session_id ON makeup_requests(target_session_id);
CREATE INDEX idx_makeup_request_status ON makeup_requests(status);
