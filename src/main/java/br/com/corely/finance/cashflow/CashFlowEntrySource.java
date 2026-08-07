package br.com.corely.finance.cashflow;

/**
 * Origem de um movimento de caixa (EPIC-03-S11).
 *
 * <p>{@code PAYMENT}: entrada originada de um pagamento registrado na baixa
 * manual. {@code MANUAL}: entrada registrada diretamente no fluxo de caixa
 * (ex.: aporte, receita extra).</p>
 */
public enum CashFlowEntrySource {
    PAYMENT,
    MANUAL
}
