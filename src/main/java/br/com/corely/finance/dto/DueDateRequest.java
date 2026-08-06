package br.com.corely.finance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Requisição de atualização da data de vencimento (EPIC-03-S04).
 */
public class DueDateRequest {

    @NotNull
    private LocalDate dueDate;

    public DueDateRequest() {
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
