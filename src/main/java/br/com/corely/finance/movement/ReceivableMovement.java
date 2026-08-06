package br.com.corely.finance.movement;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.finance.receivable.Receivable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimentação no histórico de um recebível (EPIC-03-S05).
 *
 * <p>Registra eventos do ciclo de vida do recebível (criação, pagamento, ajuste,
 * cancelamento, mudança de vencimento) com valor, descrição e data. Imutável
 * por natureza — um registro nunca é alterado.</p>
 */
@Entity(name = "ReceivableMovement")
@Table(name = "receivable_movements")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class ReceivableMovement extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public ReceivableMovement() {
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public void setReceivable(Receivable receivable) {
        this.receivable = receivable;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
