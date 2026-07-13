package br.com.corely.comercial.invoicegeneration;

public class InvoiceGenerationResult {

    private int processed;
    private int generated;
    private int skipped;
    private int errors;

    public InvoiceGenerationResult() {}

    public InvoiceGenerationResult(int processed, int generated, int skipped, int errors) {
        this.processed = processed;
        this.generated = generated;
        this.skipped = skipped;
        this.errors = errors;
    }

    public int getProcessed() { return processed; }
    public int getGenerated() { return generated; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }

    public void incrementProcessed() { this.processed++; }
    public void incrementGenerated() { this.generated++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors() { this.errors++; }
}
