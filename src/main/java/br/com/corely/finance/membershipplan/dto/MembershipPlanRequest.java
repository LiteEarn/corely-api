package br.com.corely.finance.membershipplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class MembershipPlanRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    @Positive
    private BigDecimal monthlyPrice;

    @NotNull
    @Positive
    private Integer sessionsPerWeek;

    public MembershipPlanRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public Integer getSessionsPerWeek() { return sessionsPerWeek; }
    public void setSessionsPerWeek(Integer sessionsPerWeek) { this.sessionsPerWeek = sessionsPerWeek; }
}
