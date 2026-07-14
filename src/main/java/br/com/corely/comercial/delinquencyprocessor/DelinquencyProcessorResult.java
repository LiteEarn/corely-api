package br.com.corely.comercial.delinquencyprocessor;

public class DelinquencyProcessorResult {

    private int processed;
    private int suspended;
    private int blocked;
    private int skipped;
    private int errors;

    public DelinquencyProcessorResult() {}

    public int getProcessed() { return processed; }
    public int getSuspended() { return suspended; }
    public int getBlocked() { return blocked; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }

    public void incrementProcessed() { this.processed++; }
    public void incrementSuspended() { this.suspended++; }
    public void incrementBlocked() { this.blocked++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors() { this.errors++; }
}