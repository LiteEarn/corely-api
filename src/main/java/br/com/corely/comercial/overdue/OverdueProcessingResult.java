package br.com.corely.comercial.overdue;

public class OverdueProcessingResult {

    private int processed;
    private int overdue;
    private int skipped;
    private int errors;

    public OverdueProcessingResult() {}

    public OverdueProcessingResult(int processed, int overdue, int skipped, int errors) {
        this.processed = processed;
        this.overdue = overdue;
        this.skipped = skipped;
        this.errors = errors;
    }

    public int getProcessed() { return processed; }
    public int getOverdue() { return overdue; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }

    public void incrementProcessed() { this.processed++; }
    public void incrementOverdue() { this.overdue++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors() { this.errors++; }
}
