package br.com.corely.comercial.contractreactivation;

public class ContractReactivationResult {

    private int processed;
    private int reactivated;
    private int skipped;
    private int errors;

    public ContractReactivationResult() {}

    public int getProcessed() { return processed; }
    public int getReactivated() { return reactivated; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }

    public void incrementProcessed() { this.processed++; }
    public void incrementReactivated() { this.reactivated++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors() { this.errors++; }
}