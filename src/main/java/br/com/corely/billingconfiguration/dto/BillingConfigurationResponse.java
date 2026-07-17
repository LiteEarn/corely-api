package br.com.corely.billingconfiguration.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BillingConfigurationResponse {

    private UUID id;
    private UUID studioId;
    private Integer dueDay;
    private BigDecimal defaultAmount;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BillingConfigurationResponse() {}

    public BillingConfigurationResponse(UUID id, UUID studioId, Integer dueDay, BigDecimal defaultAmount,
                                         Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studioId = studioId;
        this.dueDay = dueDay;
        this.defaultAmount = defaultAmount;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudioId() { return studioId; }
    public Integer getDueDay() { return dueDay; }
    public BigDecimal getDefaultAmount() { return defaultAmount; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
