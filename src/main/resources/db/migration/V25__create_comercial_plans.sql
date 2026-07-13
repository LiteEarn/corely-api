CREATE TABLE comercial_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    duration INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_plan_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT chk_comercial_plan_price CHECK (price > 0),
    CONSTRAINT chk_comercial_plan_duration CHECK (duration > 0)
);

CREATE INDEX idx_comercial_plan_studio_id ON comercial_plans(studio_id);
CREATE INDEX idx_comercial_plan_active ON comercial_plans(active);
CREATE INDEX idx_comercial_plan_name ON comercial_plans(name);
