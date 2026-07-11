CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    value DECIMAL(10,2) NOT NULL,
    quantity_aulas INTEGER,
    duration INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_plan_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT chk_plan_value CHECK (value > 0),
    CONSTRAINT chk_plan_duration CHECK (duration > 0),
    CONSTRAINT chk_plan_quantity_aulas CHECK (quantity_aulas IS NULL OR quantity_aulas > 0)
);

CREATE INDEX idx_plan_studio_id ON plans(studio_id);
CREATE INDEX idx_plan_active ON plans(active);
CREATE INDEX idx_plan_name ON plans(name);
