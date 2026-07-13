package br.com.corely.comercial.plan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PlanResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal value;
    private Integer duration;
    private Integer version;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PlanResponse() {}

    public PlanResponse(UUID id, String name, String description, BigDecimal value,
                        Integer duration, Integer version, Boolean active,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.value = value;
        this.duration = duration;
        this.version = version;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getValue() { return value; }
    public Integer getDuration() { return duration; }
    public Integer getVersion() { return version; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
