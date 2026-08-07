-- V10__cash_flow_entries.sql
-- Fluxo de Caixa — Entradas (EPIC-03-S11)

CREATE TABLE corely.cash_flow_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    entry_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(500),
    source VARCHAR(20) NOT NULL,
    payment_id UUID,
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cash_flow_entry_studio FOREIGN KEY (studio_id) REFERENCES corely.studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_cash_flow_entry_payment FOREIGN KEY (payment_id) REFERENCES corely.payments(id) ON DELETE SET NULL,
    CONSTRAINT chk_cash_flow_entry_type CHECK (entry_type IN ('ENTRY', 'OUTFLOW')),
    CONSTRAINT chk_cash_flow_entry_source CHECK (source IN ('PAYMENT', 'MANUAL')),
    CONSTRAINT chk_cash_flow_entry_amount CHECK (amount > 0)
);

CREATE INDEX idx_cash_flow_entry_studio_id ON corely.cash_flow_entries(studio_id);
CREATE INDEX idx_cash_flow_entry_entry_date ON corely.cash_flow_entries(entry_date);
CREATE INDEX idx_cash_flow_entry_type ON corely.cash_flow_entries(entry_type);
