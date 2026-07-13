CREATE TABLE comercial_plan_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    studio_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    rule_definition_id UUID NOT NULL,
    rule_value VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comercial_plan_rule_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_plan_rule_plan FOREIGN KEY (plan_id) REFERENCES comercial_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_comercial_plan_rule_rule_definition FOREIGN KEY (rule_definition_id) REFERENCES comercial_rule_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uq_comercial_plan_rule_plan_rule_def UNIQUE (plan_id, rule_definition_id)
);

CREATE INDEX idx_comercial_plan_rule_studio_id ON comercial_plan_rules(studio_id);
CREATE INDEX idx_comercial_plan_rule_plan_id ON comercial_plan_rules(plan_id);
CREATE INDEX idx_comercial_plan_rule_rule_definition_id ON comercial_plan_rules(rule_definition_id);
