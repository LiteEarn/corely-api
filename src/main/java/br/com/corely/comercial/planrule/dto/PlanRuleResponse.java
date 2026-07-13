package br.com.corely.comercial.planrule.dto;

import br.com.corely.comercial.ruledefinition.ValueType;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlanRuleResponse {

    private UUID id;
    private UUID planId;
    private UUID ruleDefinitionId;
    private String ruleDefinitionCode;
    private String ruleDefinitionName;
    private ValueType valueType;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PlanRuleResponse() {}

    public PlanRuleResponse(UUID id, UUID planId, UUID ruleDefinitionId,
                            String ruleDefinitionCode, String ruleDefinitionName,
                            ValueType valueType, String value,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.planId = planId;
        this.ruleDefinitionId = ruleDefinitionId;
        this.ruleDefinitionCode = ruleDefinitionCode;
        this.ruleDefinitionName = ruleDefinitionName;
        this.valueType = valueType;
        this.value = value;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public UUID getRuleDefinitionId() { return ruleDefinitionId; }
    public String getRuleDefinitionCode() { return ruleDefinitionCode; }
    public String getRuleDefinitionName() { return ruleDefinitionName; }
    public ValueType getValueType() { return valueType; }
    public String getValue() { return value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
