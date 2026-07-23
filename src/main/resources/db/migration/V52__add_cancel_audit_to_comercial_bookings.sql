ALTER TABLE comercial_bookings ADD COLUMN cancel_reason VARCHAR(20) NULL;
ALTER TABLE comercial_bookings ADD COLUMN cancel_description TEXT NULL;
ALTER TABLE comercial_bookings ADD COLUMN cancelled_by UUID NULL;
ALTER TABLE comercial_bookings ADD COLUMN cancelled_at TIMESTAMP NULL;
