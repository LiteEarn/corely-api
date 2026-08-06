package br.com.corely.finance.situation;

import java.time.LocalDate;

/**
 * Situação financeira de um recebível ou parcela (EPIC-03-S03).
 *
 * <p>É derivada do status persistido e do vencimento:
 * <ul>
 *   <li>{@code OPEN} — em aberto e não vencido</li>
 *   <li>{@code PAID} — pago</li>
 *   <li>{@code OVERDUE} — em aberto e vencido</li>
 *   <li>{@code REVERSED} — estornado/cancelado</li>
 * </ul>
 * </p>
 */
public enum Situation {
    OPEN,
    PAID,
    OVERDUE,
    REVERSED;

    /**
     * Deriva a situação a partir do status persistido e da data de vencimento.
     *
     * <p>Espera os valores de {@code name()} dos enums controlados do domínio
     * financeiro ({@code ReceivableStatus} / {@code InstallmentStatus}):
     * {@code OPEN}, {@code PAID} ou {@code CANCELLED}. Qualquer outro valor
     * cai no branch de "em aberto".</p>
     *
     * @param status   status persistido (OPEN/PAID/CANCELLED)
     * @param dueDate  data de vencimento
     * @return situação financeira calculada
     */
    public static Situation from(String status, LocalDate dueDate) {
        return switch (status) {
            case "PAID" -> PAID;
            case "CANCELLED" -> REVERSED;
            default -> (dueDate != null && dueDate.isBefore(LocalDate.now())) ? OVERDUE : OPEN;
        };
    }
}
