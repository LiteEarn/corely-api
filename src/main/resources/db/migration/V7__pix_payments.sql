-- V7__pix_payments.sql
-- Pagamentos — Pix (EPIC-03-S07)

CREATE TABLE corely.pix_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    receivable_id UUID NOT NULL,
    txid VARCHAR(64) NOT NULL,
    copy_paste VARCHAR(512) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pix_payment_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_pix_payment_receivable FOREIGN KEY (receivable_id) REFERENCES corely.receivables(id) ON DELETE CASCADE,
    CONSTRAINT uq_pix_payment_txid UNIQUE (txid),
    CONSTRAINT uq_pix_payment_receivable UNIQUE (receivable_id),
    CONSTRAINT chk_pix_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_pix_payment_status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_pix_payment_studio_id ON corely.pix_payments(studio_id);
CREATE INDEX idx_pix_payment_receivable_id ON corely.pix_payments(receivable_id);
CREATE INDEX idx_pix_payment_status ON corely.pix_payments(status);