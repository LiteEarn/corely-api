package br.com.corely.comercial.planrule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class PlanRuleRequest {

    @NotNull
    private UUID ruleDefinitionId;

    @NotBlank
    @Size(max = 500)
    private String value;

    public PlanRuleRequest() {}

    public UUID getRuleDefinitionId() { return ruleDefinitionId; }
    public void setRuleDefinitionId(UUID ruleDefinitionId) { this.ruleDefinitionId = ruleDefinitionId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
