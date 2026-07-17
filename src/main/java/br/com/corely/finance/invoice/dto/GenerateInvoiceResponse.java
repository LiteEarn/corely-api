package br.com.corely.finance.invoice.dto;

public class GenerateInvoiceResponse {

    private int created;
    private int ignored;
    private long executionTimeMillis;

    public GenerateInvoiceResponse() {}

    public GenerateInvoiceResponse(int created, int ignored, long executionTimeMillis) {
        this.created = created;
        this.ignored = ignored;
        this.executionTimeMillis = executionTimeMillis;
    }

    public int getCreated() { return created; }
    public int getIgnored() { return ignored; }
    public long getExecutionTimeMillis() { return executionTimeMillis; }
}
