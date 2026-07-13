CREATE TABLE comercial_student_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    contract_snapshot_id UUID NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    cancellation_date DATE,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_student_plan_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_student_plan_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_student_plan_snapshot FOREIGN KEY (contract_snapshot_id) REFERENCES comercial_contract_snapshots(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_student_plan_active_per_student UNIQUE (student_id, status)
);

CREATE INDEX idx_comercial_student_plan_studio_id ON comercial_student_plans(studio_id);
CREATE INDEX idx_comercial_student_plan_student_id ON comercial_student_plans(student_id);
CREATE INDEX idx_comercial_student_plan_status ON comercial_student_plans(status);
CREATE INDEX idx_comercial_student_plan_start_date ON comercial_student_plans(start_date);
