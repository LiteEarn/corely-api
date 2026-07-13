package br.com.corely.comercial.billingschedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BillingScheduleRequest {

    @NotNull
    private BillingFrequencyDto frequency;

    @NotNull
    @Min(1)
    @Max(31)
    private Integer billingDay;

    private Boolean active;

    public BillingScheduleRequest() {}

    public BillingFrequencyDto getFrequency() { return frequency; }
    public void setFrequency(BillingFrequencyDto frequency) { this.frequency = frequency; }
    public Integer getBillingDay() { return billingDay; }
    public void setBillingDay(Integer billingDay) { this.billingDay = billingDay; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
