-- V8__card_payments.sql
-- Pagamentos — Cartão (EPIC-03-S08)

CREATE TABLE corely.card_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    receivable_id UUID NOT NULL,
    transaction_id VARCHAR(64) NOT NULL,
    card_brand VARCHAR(32) NOT NULL,
    last_four_digits VARCHAR(4) NOT NULL,
    installments INT NOT NULL DEFAULT 1,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_payment_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_payment_receivable FOREIGN KEY (receivable_id) REFERENCES corely.receivables(id) ON DELETE CASCADE,
    CONSTRAINT uq_card_payment_transaction_id UNIQUE (transaction_id),
    CONSTRAINT uq_card_payment_receivable UNIQUE (receivable_id),
    CONSTRAINT chk_card_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_card_payment_installments CHECK (installments > 0),
    CONSTRAINT chk_card_payment_status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_card_payment_studio_id ON corely.card_payments(studio_id);
CREATE INDEX idx_card_payment_receivable_id ON corely.card_payments(receivable_id);
CREATE INDEX idx_card_payment_status ON corely.card_payments(status);