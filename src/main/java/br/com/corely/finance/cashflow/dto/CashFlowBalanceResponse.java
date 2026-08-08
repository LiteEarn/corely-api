package br.com.corely.finance.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Saldo do fluxo de caixa do estúdio (EPIC-03-S13).
 *
 * <p>Saldo = total de entradas ({@code ENTRY}) − total de saídas ({@code OUTFLOW}),
 * considerando apenas os lançamentos do estúdio corrente no período informado
 * (quando aplicável).</p>
 */
public class CashFlowBalanceResponse {

    private BigDecimal totalEntries;
    private BigDecimal totalOutflows;
    private BigDecimal balance;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    public CashFlowBalanceResponse() {
    }

    public CashFlowBalanceResponse(BigDecimal totalEntries, BigDecimal totalOutflows,
                                   BigDecimal balance, LocalDate dateFrom, LocalDate dateTo) {
        this.totalEntries = totalEntries;
        this.totalOutflows = totalOutflows;
        this.balance = balance;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public BigDecimal getTotalEntries() { return totalEntries; }
    public BigDecimal getTotalOutflows() { return totalOutflows; }
    public BigDecimal getBalance() { return balance; }
    public LocalDate getDateFrom() { return dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
}
