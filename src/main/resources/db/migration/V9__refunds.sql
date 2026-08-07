-- V9__refunds.sql
-- Estorno de pagamento (EPIC-03-S10)

-- Marca o momento em que o pagamento foi estornado. Quando preenchido, o
-- pagamento foi estornado e o recebível/parcela retornaram à situação de aberto.
ALTER TABLE corely.payments ADD COLUMN refunded_at TIMESTAMP;

CREATE INDEX idx_payment_refunded_at ON corely.payments(refunded_at);

-- O estorno registra uma movimentação REFUND no histórico do recebível
-- (corely.receivable_movements). A constraint de CHECK criada na V5 precisa ser
-- atualizada para aceitar o novo tipo de movimentação.
ALTER TABLE corely.receivable_movements DROP CONSTRAINT chk_receivable_movement_type;
ALTER TABLE corely.receivable_movements ADD CONSTRAINT chk_receivable_movement_type
    CHECK (movement_type IN ('CREATED', 'PAYMENT', 'ADJUSTMENT', 'CANCELLED', 'DUE_DATE_CHANGED', 'REFUND'));
