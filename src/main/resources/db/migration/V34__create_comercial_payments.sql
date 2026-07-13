CREATE TABLE comercial_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    external_reference VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_payment_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_payment_invoice FOREIGN KEY (invoice_id) REFERENCES comercial_invoices(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_payment_invoice UNIQUE (invoice_id)
);

CREATE INDEX idx_comercial_payment_studio_id ON comercial_payments(studio_id);
CREATE INDEX idx_comercial_payment_invoice_id ON comercial_payments(invoice_id);
CREATE INDEX idx_comercial_payment_payment_date ON comercial_payments(payment_date);
CREATE INDEX idx_comercial_payment_payment_method ON comercial_payments(payment_method);
