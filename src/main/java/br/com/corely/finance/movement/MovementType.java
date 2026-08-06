package br.com.corely.finance.movement;

/**
 * Tipo de movimentação no histórico de um recebível (EPIC-03-S05).
 */
public enum MovementType {
    CREATED,
    PAYMENT,
    ADJUSTMENT,
    CANCELLED,
    DUE_DATE_CHANGED
}
