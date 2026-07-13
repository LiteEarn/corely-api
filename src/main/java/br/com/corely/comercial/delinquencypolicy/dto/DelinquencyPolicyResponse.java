package br.com.corely.comercial.delinquencypolicy.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DelinquencyPolicyResponse {

    private UUID id;
    private Integer gracePeriodDays;
    private DelinquencyActionDto action;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DelinquencyPolicyResponse() {}

    public DelinquencyPolicyResponse(UUID id, Integer gracePeriodDays, DelinquencyActionDto action,
                                     Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.gracePeriodDays = gracePeriodDays;
        this.action = action;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public DelinquencyActionDto getAction() { return action; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
