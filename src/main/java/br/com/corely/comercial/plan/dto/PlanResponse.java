package br.com.corely.comercial.plan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlanResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private Integer version;
    private Boolean active;
    private Boolean autoRenew;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PlanResponse() {}

    public PlanResponse(UUID id, String name, String description, BigDecimal price,
                        Integer duration, Integer version, Boolean active, Boolean autoRenew,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.duration = duration;
        this.version = version;
        this.active = active;
        this.autoRenew = autoRenew;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getDuration() { return duration; }
    public Integer getVersion() { return version; }
    public Boolean getActive() { return active; }
    public Boolean getAutoRenew() { return autoRenew; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
