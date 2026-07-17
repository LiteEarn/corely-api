-- BillingConfiguration: one per studio
CREATE TABLE billing_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL UNIQUE,
    due_day INTEGER NOT NULL CHECK (due_day >= 1 AND due_day <= 31),
    default_amount DECIMAL(10, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_billing_configuration_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_billing_configuration_studio_id ON billing_configurations(studio_id);

-- Add billing_enabled to students
ALTER TABLE students ADD COLUMN billing_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Add billing_month to finance_invoices
ALTER TABLE finance_invoices ADD COLUMN billing_month VARCHAR(7);

CREATE INDEX idx_finance_invoice_billing_month ON finance_invoices(billing_month);

-- Unique constraint to prevent duplicate invoices per student + month + studio
CREATE UNIQUE INDEX idx_finance_invoice_unique_student_month
    ON finance_invoices(student_id, billing_month, studio_id)
    WHERE billing_month IS NOT NULL;
