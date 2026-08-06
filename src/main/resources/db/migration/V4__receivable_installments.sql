-- V4__receivable_installments.sql
-- Contas a Receber — Parcelas (EPIC-03-S02)

CREATE TABLE corely.receivable_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    receivable_id UUID NOT NULL,
    student_plan_id UUID NOT NULL,
    installment_number INTEGER NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receivable_installment_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_receivable_installment_receivable FOREIGN KEY (receivable_id) REFERENCES corely.receivables(id) ON DELETE CASCADE,
    CONSTRAINT fk_receivable_installment_student_plan FOREIGN KEY (student_plan_id) REFERENCES corely.comercial_student_plans(id) ON DELETE CASCADE,
    CONSTRAINT chk_receivable_installment_number CHECK (installment_number > 0),
    CONSTRAINT chk_receivable_installment_amount CHECK (amount >= 0),
    CONSTRAINT chk_receivable_installment_status CHECK (status IN ('OPEN', 'PAID', 'CANCELLED'))
);

CREATE INDEX idx_receivable_installment_studio_id ON corely.receivable_installments(studio_id);
CREATE INDEX idx_receivable_installment_receivable_id ON corely.receivable_installments(receivable_id);
CREATE INDEX idx_receivable_installment_student_plan_id ON corely.receivable_installments(student_plan_id);
CREATE INDEX idx_receivable_installment_status ON corely.receivable_installments(status);
CREATE INDEX idx_receivable_installment_due_date ON corely.receivable_installments(due_date);
