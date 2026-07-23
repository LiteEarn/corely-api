ALTER TABLE comercial_class_sessions ADD COLUMN cancel_reason VARCHAR(30) NULL;
ALTER TABLE comercial_class_sessions ADD COLUMN cancel_description TEXT NULL;
ALTER TABLE comercial_class_sessions ADD COLUMN cancelled_by UUID NULL;
ALTER TABLE comercial_class_sessions ADD COLUMN cancelled_at TIMESTAMP NULL;
