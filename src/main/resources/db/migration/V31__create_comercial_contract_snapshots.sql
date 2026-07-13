CREATE TABLE comercial_contract_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL,
    plan_version INTEGER NOT NULL,
    plan_name VARCHAR(255) NOT NULL,
    plan_description TEXT,
    plan_price DECIMAL(10, 2) NOT NULL,
    plan_duration INTEGER NOT NULL,
    rules TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comercial_contract_snapshot_plan_id ON comercial_contract_snapshots(plan_id);
CREATE INDEX idx_comercial_contract_snapshot_created_at ON comercial_contract_snapshots(created_at);
