package br.com.corely.billingconfiguration;

import br.com.corely.comercial.ComercialBaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;

@Entity
@Table(name = "billing_configurations")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class BillingConfiguration extends ComercialBaseEntity {

    @Column(name = "due_day", nullable = false)
    private Integer dueDay;

    @Column(name = "default_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultAmount;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public BillingConfiguration() {}

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
