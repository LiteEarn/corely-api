CREATE TABLE comercial_rule_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    value_type VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comercial_rule_definition_code UNIQUE (code),
    CONSTRAINT chk_comercial_rule_definition_value_type CHECK (value_type IN ('BOOLEAN', 'INTEGER', 'DECIMAL', 'STRING', 'ENUM')),
    CONSTRAINT chk_comercial_rule_definition_category CHECK (category IN ('VALIDITY', 'ATTENDANCE', 'BILLING', 'BOOKING', 'CANCELLATION', 'GENERAL'))
);

CREATE INDEX idx_comercial_rule_definition_active ON comercial_rule_definitions(active);
CREATE INDEX idx_comercial_rule_definition_code ON comercial_rule_definitions(code);
CREATE INDEX idx_comercial_rule_definition_category ON comercial_rule_definitions(category);
