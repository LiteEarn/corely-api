package br.com.corely.finance.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projeção do fluxo de caixa do estúdio (EPIC-03-S14).
 *
 * <p>Projeta o caixa disponível em um horizonte futuro: saldo atual
 * (entradas − saídas até hoje) + recebíveis em aberto a vencer no horizonte
 * (entradas futuras esperadas) − saídas futuras planejadas no horizonte.</p>
 */
public class CashFlowProjectionResponse {

    private BigDecimal currentBalance;
    private BigDecimal projectedEntries;
    private BigDecimal projectedOutflows;
    private BigDecimal projectedBalance;
    private LocalDate horizonDate;

    public CashFlowProjectionResponse() {
    }

    public CashFlowProjectionResponse(BigDecimal currentBalance, BigDecimal projectedEntries,
                                      BigDecimal projectedOutflows, BigDecimal projectedBalance,
                                      LocalDate horizonDate) {
        this.currentBalance = currentBalance;
        this.projectedEntries = projectedEntries;
        this.projectedOutflows = projectedOutflows;
        this.projectedBalance = projectedBalance;
        this.horizonDate = horizonDate;
    }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getProjectedEntries() { return projectedEntries; }
    public BigDecimal getProjectedOutflows() { return projectedOutflows; }
    public BigDecimal getProjectedBalance() { return projectedBalance; }
    public LocalDate getHorizonDate() { return horizonDate; }
}