CREATE TABLE comercial_delinquency_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    grace_period_days INTEGER NOT NULL DEFAULT 0,
    action VARCHAR(30) NOT NULL DEFAULT 'NONE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_delinquency_policy_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_delinquency_policy_studio UNIQUE (studio_id),
    CONSTRAINT chk_comercial_delinquency_policy_grace_period CHECK (grace_period_days >= 0)
);

CREATE INDEX idx_comercial_delinquency_policy_studio_id ON comercial_delinquency_policies(studio_id);
CREATE INDEX idx_comercial_delinquency_policy_active ON comercial_delinquency_policies(active);
