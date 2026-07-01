ALTER TABLE makeup_requests
    ADD COLUMN rejected_at TIMESTAMP,
    ADD COLUMN rejection_reason VARCHAR(500);