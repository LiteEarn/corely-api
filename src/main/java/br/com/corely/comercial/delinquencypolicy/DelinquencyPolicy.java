package br.com.corely.comercial.delinquencypolicy;

import br.com.corely.comercial.ComercialBaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "comercial_delinquency_policies")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class DelinquencyPolicy extends ComercialBaseEntity {

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private DelinquencyAction action = DelinquencyAction.NONE;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public DelinquencyPolicy() {}

    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public DelinquencyAction getAction() { return action; }
    public void setAction(DelinquencyAction action) { this.action = action; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
