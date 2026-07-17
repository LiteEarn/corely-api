package br.com.corely.billingconfiguration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BillingConfigurationRequest {

    @NotNull
    @Min(1)
    @Max(31)
    private Integer dueDay;

    @NotNull
    @Positive
    private BigDecimal defaultAmount;

    private Boolean active;

    public BillingConfigurationRequest() {}

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(BigDecimal defaultAmount) { this.defaultAmount = defaultAmount; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
