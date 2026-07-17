package br.com.corely.finance.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class GenerateInvoiceRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Month must be in YYYY-MM format")
    private String month;

    public GenerateInvoiceRequest() {}

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
}
