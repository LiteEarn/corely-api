package br.com.corely.comercial.contractexpiration;

public class ContractExpirationResult {

    private int processed;
    private int finished;
    private int skipped;
    private int errors;

    public ContractExpirationResult() {}

    public int getProcessed() { return processed; }
    public int getFinished() { return finished; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }

    public void incrementProcessed() { this.processed++; }
    public void incrementFinished() { this.finished++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors() { this.errors++; }
}
