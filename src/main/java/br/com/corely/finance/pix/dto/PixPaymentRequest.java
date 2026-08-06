package br.com.corely.finance.pix.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Requisição de geração de cobrança Pix (EPIC-03-S07).
 *
 * <p>Cria uma cobrança Pix para um recebível em aberto do estúdio corrente,
 * com validade opcional (o padrão é 24 horas).</p>
 */
public class PixPaymentRequest {

    @NotNull
    private UUID receivableId;

    @Future
    private LocalDateTime expiresAt;

    public PixPaymentRequest() {
    }

    public UUID getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(UUID receivableId) {
        this.receivableId = receivableId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}