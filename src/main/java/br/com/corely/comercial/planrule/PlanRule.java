package br.com.corely.comercial.planrule;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.ruledefinition.RuleDefinition;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "comercial_plan_rules",
       uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "rule_definition_id"}))
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class PlanRule extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_definition_id", nullable = false)
    private RuleDefinition ruleDefinition;

    @Column(name = "rule_value", nullable = false, length = 500)
    private String value;

    protected PlanRule() {}

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public RuleDefinition getRuleDefinition() { return ruleDefinition; }
    public void setRuleDefinition(RuleDefinition ruleDefinition) { this.ruleDefinition = ruleDefinition; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
