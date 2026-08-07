-- V9__refunds.sql
-- Estorno de pagamento (EPIC-03-S10)

-- Marca o momento em que o pagamento foi estornado. Quando preenchido, o
-- pagamento foi estornado e o recebível/parcela retornaram à situação de aberto.
ALTER TABLE corely.payments ADD COLUMN refunded_at TIMESTAMP;

CREATE INDEX idx_payment_refunded_at ON corely.payments(refunded_at);
