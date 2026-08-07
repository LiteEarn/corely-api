package br.com.corely.finance.card;

/**
 * Situação de uma transação de cartão (EPIC-03-S08).
 *
 * <p>Representa o ciclo de vida de uma transação de cartão gerada para um
 * recebível: criada ({@code PENDING}), confirmada/paga ({@code PAID}), expirada
 * ({@code EXPIRED}) ou cancelada ({@code CANCELLED}).</p>
 */
public enum CardPaymentStatus {
    PENDING,
    PAID,
    EXPIRED,
    CANCELLED
}