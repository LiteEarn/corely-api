package br.com.corely.finance.payment.dto;

/**
 * Forma de pagamento aceita na baixa manual (EPIC-03-S06).
 *
 * <p>Espelha {@link br.com.corely.finance.payment.PaymentMethod} para o contrato
 * de API, mantendo o DTO desacoplado da entidade.</p>
 */
public enum PaymentMethodDto {
    CASH,
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    OTHER
}