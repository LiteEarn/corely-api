package br.com.corely.finance.membershipplan;

import br.com.corely.comercial.ComercialBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;

@Entity
@Table(name = "membership_plans")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class MembershipPlan extends ComercialBaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "sessions_per_week", nullable = false)
    private Integer sessionsPerWeek;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public MembershipPlan() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public Integer getSessionsPerWeek() { return sessionsPerWeek; }
    public void setSessionsPerWeek(Integer sessionsPerWeek) { this.sessionsPerWeek = sessionsPerWeek; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
