package br.com.corely.comercial.contractsnapshot;

import br.com.corely.shared.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "comercial_contract_snapshots")
public class ContractSnapshot extends BaseEntity {

    @Column(name = "studio_id", nullable = false)
    private UUID studioId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_version", nullable = false)
    private Integer planVersion;

    @Column(name = "plan_name", nullable = false, length = 255)
    private String planName;

    @Column(name = "plan_description", columnDefinition = "TEXT")
    private String planDescription;

    @Column(name = "plan_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal planPrice;

    @Column(name = "plan_duration", nullable = false)
    private Integer planDuration;

    @Column(name = "rules", nullable = false, columnDefinition = "TEXT")
    private String rules;

    public ContractSnapshot() {}

    public UUID getStudioId() { return studioId; }
    public void setStudioId(UUID studioId) { this.studioId = studioId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public Integer getPlanVersion() { return planVersion; }
    public void setPlanVersion(Integer planVersion) { this.planVersion = planVersion; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getPlanDescription() { return planDescription; }
    public void setPlanDescription(String planDescription) { this.planDescription = planDescription; }
    public BigDecimal getPlanPrice() { return planPrice; }
    public void setPlanPrice(BigDecimal planPrice) { this.planPrice = planPrice; }
    public Integer getPlanDuration() { return planDuration; }
    public void setPlanDuration(Integer planDuration) { this.planDuration = planDuration; }
    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }
}
