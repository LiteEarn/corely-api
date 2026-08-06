package br.com.corely.finance.pix;

/**
 * Situação de uma cobrança Pix (EPIC-03-S07).
 *
 * <p>Representa o ciclo de vida de uma cobrança Pix gerada para um recebível:
 * criada ({@code PENDING}), confirmada/paga ({@code PAID}), expirada
 * ({@code EXPIRED}) ou cancelada ({@code CANCELLED}).</p>
 */
public enum PixPaymentStatus {
    PENDING,
    PAID,
    EXPIRED,
    CANCELLED
}