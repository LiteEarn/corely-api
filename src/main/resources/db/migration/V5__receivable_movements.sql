-- V5__receivable_movements.sql
-- Contas a Receber — Histórico de movimentações (EPIC-03-S05)

CREATE TABLE corely.receivable_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    receivable_id UUID NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    amount DECIMAL(10, 2),
    description VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receivable_movement_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_receivable_movement_receivable FOREIGN KEY (receivable_id) REFERENCES corely.receivables(id) ON DELETE CASCADE,
    CONSTRAINT chk_receivable_movement_type CHECK (movement_type IN ('CREATED', 'PAYMENT', 'ADJUSTMENT', 'CANCELLED', 'DUE_DATE_CHANGED'))
);

CREATE INDEX idx_receivable_movement_studio_id ON corely.receivable_movements(studio_id);
CREATE INDEX idx_receivable_movement_receivable_id ON corely.receivable_movements(receivable_id);
CREATE INDEX idx_receivable_movement_type ON corely.receivable_movements(movement_type);
CREATE INDEX idx_receivable_movement_occurred_at ON corely.receivable_movements(occurred_at);
