package br.com.corely.finance.membershipplan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class MembershipPlanResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private Integer sessionsPerWeek;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MembershipPlanResponse() {}

    public MembershipPlanResponse(UUID id, String name, String description, BigDecimal monthlyPrice,
                                  Integer sessionsPerWeek, Boolean active,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.monthlyPrice = monthlyPrice;
        this.sessionsPerWeek = sessionsPerWeek;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public Integer getSessionsPerWeek() { return sessionsPerWeek; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
