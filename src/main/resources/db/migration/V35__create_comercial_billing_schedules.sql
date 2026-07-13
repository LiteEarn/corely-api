CREATE TABLE comercial_billing_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_plan_id UUID NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    billing_day INTEGER NOT NULL,
    next_billing_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_billing_schedule_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_billing_schedule_student_plan FOREIGN KEY (student_plan_id) REFERENCES comercial_student_plans(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_billing_schedule_student_plan UNIQUE (student_plan_id),
    CONSTRAINT chk_comercial_billing_schedule_billing_day CHECK (billing_day >= 1 AND billing_day <= 31)
);

CREATE INDEX idx_comercial_billing_schedule_studio_id ON comercial_billing_schedules(studio_id);
CREATE INDEX idx_comercial_billing_schedule_next_billing_date ON comercial_billing_schedules(next_billing_date);
CREATE INDEX idx_comercial_billing_schedule_active ON comercial_billing_schedules(active);
CREATE INDEX idx_comercial_billing_schedule_frequency ON comercial_billing_schedules(frequency);
