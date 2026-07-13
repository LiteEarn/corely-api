package br.com.corely.comercial.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class InvoiceRequest {

    @NotNull
    private UUID studentPlanId;

    @NotNull
    private LocalDate dueDate;

    @NotBlank
    @Size(max = 7)
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "referenceMonth must be in yyyy-MM format")
    private String referenceMonth;

    public InvoiceRequest() {}

    public UUID getStudentPlanId() { return studentPlanId; }
    public void setStudentPlanId(UUID studentPlanId) { this.studentPlanId = studentPlanId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReferenceMonth() { return referenceMonth; }
    public void setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; }
}
