package br.com.corely.finance.card.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Requisição de geração de transação de cartão (EPIC-03-S08).
 *
 * <p>Cria uma transação de cartão para um recebível em aberto do estúdio
 * corrente, com bandeira, últimos quatro dígitos do cartão e número de parcelas.
 * A validade é opcional (o padrão é 24 horas).</p>
 *
 * <p>Não recebe dados completos do cartão (número, CVV, validade) — apenas a
 * bandeira e os últimos quatro dígitos, em conformidade com a PCI DSS.</p>
 */
public class CardPaymentRequest {

    @NotNull
    private UUID receivableId;

    @NotBlank
    @Size(max = 32)
    private String cardBrand;

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "lastFourDigits must have exactly 4 digits")
    private String lastFourDigits;

    @Min(1)
    @Max(12)
    private Integer installments = 1;

    @Future
    private LocalDateTime expiresAt;

    public CardPaymentRequest() {
    }

    public UUID getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(UUID receivableId) {
        this.receivableId = receivableId;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}