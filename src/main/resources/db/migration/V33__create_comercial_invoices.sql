CREATE TABLE comercial_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_plan_id UUID NOT NULL,
    due_date DATE NOT NULL,
    reference_month VARCHAR(7) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_invoice_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_invoice_student_plan FOREIGN KEY (student_plan_id) REFERENCES comercial_student_plans(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_invoice_student_plan_month UNIQUE (student_plan_id, reference_month)
);

CREATE INDEX idx_comercial_invoice_studio_id ON comercial_invoices(studio_id);
CREATE INDEX idx_comercial_invoice_student_plan_id ON comercial_invoices(student_plan_id);
CREATE INDEX idx_comercial_invoice_due_date ON comercial_invoices(due_date);
CREATE INDEX idx_comercial_invoice_status ON comercial_invoices(status);
CREATE INDEX idx_comercial_invoice_reference_month ON comercial_invoices(reference_month);
