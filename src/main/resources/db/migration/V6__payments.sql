-- V6__payments.sql
-- Pagamentos — Baixa manual (EPIC-03-S06)

CREATE TABLE corely.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    receivable_id UUID NOT NULL,
    installment_id UUID,
    payment_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    external_reference VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_receivable FOREIGN KEY (receivable_id) REFERENCES corely.receivables(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_installment FOREIGN KEY (installment_id) REFERENCES corely.receivable_installments(id) ON DELETE CASCADE,
    CONSTRAINT uq_payment_receivable UNIQUE (receivable_id),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH', 'PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'OTHER'))
);

CREATE INDEX idx_payment_studio_id ON corely.payments(studio_id);
CREATE INDEX idx_payment_receivable_id ON corely.payments(receivable_id);
CREATE INDEX idx_payment_installment_id ON corely.payments(installment_id);
CREATE INDEX idx_payment_payment_date ON corely.payments(payment_date);