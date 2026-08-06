package br.com.corely.finance.movement.dto;

import br.com.corely.finance.movement.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de uma movimentação do histórico (EPIC-03-S05).
 */
public class MovementResponse {

    private UUID id;
    private UUID receivableId;
    private MovementType movementType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime occurredAt;

    public MovementResponse() {
    }

    public MovementResponse(UUID id, UUID receivableId, MovementType movementType,
                            BigDecimal amount, String description, LocalDateTime occurredAt) {
        this.id = id;
        this.receivableId = receivableId;
        this.movementType = movementType;
        this.amount = amount;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getReceivableId() { return receivableId; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
