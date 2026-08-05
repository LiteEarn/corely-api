-- V3__receivables.sql
-- Contas a Receber — Recebíveis (EPIC-03-S01)

CREATE TABLE corely.receivables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    student_id UUID NOT NULL,
    description VARCHAR(500),
    amount DECIMAL(10, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receivable_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_receivable_student FOREIGN KEY (student_id) REFERENCES corely.students(id) ON DELETE CASCADE,
    CONSTRAINT chk_receivable_amount CHECK (amount >= 0),
    CONSTRAINT chk_receivable_status CHECK (status IN ('OPEN', 'PAID', 'CANCELLED'))
);

CREATE INDEX idx_receivable_studio_id ON corely.receivables(studio_id);
CREATE INDEX idx_receivable_student_id ON corely.receivables(student_id);
CREATE INDEX idx_receivable_status ON corely.receivables(status);
CREATE INDEX idx_receivable_due_date ON corely.receivables(due_date);
