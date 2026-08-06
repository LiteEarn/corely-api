package br.com.corely.finance.payment;

/**
 * Forma de pagamento de um recebível (EPIC-03-S06).
 *
 * <p>Representa o meio pelo qual a baixa manual foi realizada. Os métodos
 * específicos (Pix, cartão, dinheiro) são detalhados nas stories S07-S09.</p>
 */
public enum PaymentMethod {
    CASH,
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    OTHER
}