package br.com.corely.comercial.contractrenewal;

public class ContractRenewalResult {

    private int processed;
    private int renewed;
    private int skipped;
    private int errors;

    public ContractRenewalResult() {}

    public int getProcessed() { return processed; }
    public int getRenewed() { return renewed; }
    public int getSkipped() { return skipped; }
    public int getErrors() { return errors; }

    public void incrementProcessed() { this.processed++; }
    public void incrementRenewed() { this.renewed++; }
    public void incrementSkipped() { this.skipped++; }
    public void incrementErrors() { this.errors++; }
}
