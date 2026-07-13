package br.com.corely.comercial.delinquencypolicy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DelinquencyPolicyRequest {

    @NotNull
    @Min(0)
    private Integer gracePeriodDays;

    @NotNull
    private DelinquencyActionDto action;

    private Boolean active;

    public DelinquencyPolicyRequest() {}

    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public DelinquencyActionDto getAction() { return action; }
    public void setAction(DelinquencyActionDto action) { this.action = action; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
