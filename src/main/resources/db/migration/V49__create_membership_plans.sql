CREATE TABLE membership_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    monthly_price DECIMAL(10, 2) NOT NULL,
    sessions_per_week INTEGER NOT NULL CHECK (sessions_per_week > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_plan_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_membership_plan_studio_id ON membership_plans(studio_id);
CREATE INDEX idx_membership_plan_active ON membership_plans(active);

CREATE UNIQUE INDEX idx_membership_plan_unique_name_per_studio
    ON membership_plans(studio_id, name)
    WHERE active = TRUE;

ALTER TABLE students ADD COLUMN membership_plan_id UUID;

UPDATE students SET membership_plan_id = NULL;

ALTER TABLE students
    ADD CONSTRAINT fk_student_membership_plan
    FOREIGN KEY (membership_plan_id) REFERENCES membership_plans(id);
