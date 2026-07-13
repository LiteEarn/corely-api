CREATE TABLE comercial_student_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    plan_id UUID,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    snapshot_name VARCHAR(255) NOT NULL,
    snapshot_value DECIMAL(10,2) NOT NULL,
    snapshot_duration INTEGER NOT NULL,
    snapshot_rules TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_sp_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_sp_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_sp_plan FOREIGN KEY (plan_id) REFERENCES comercial_plans(id) ON DELETE SET NULL,
    CONSTRAINT chk_comercial_sp_snapshot_value CHECK (snapshot_value > 0),
    CONSTRAINT chk_comercial_sp_snapshot_duration CHECK (snapshot_duration > 0)
);

CREATE INDEX idx_comercial_sp_studio_id ON comercial_student_plans(studio_id);
CREATE INDEX idx_comercial_sp_student_id ON comercial_student_plans(student_id);
CREATE INDEX idx_comercial_sp_plan_id ON comercial_student_plans(plan_id);
CREATE INDEX idx_comercial_sp_status ON comercial_student_plans(status);
