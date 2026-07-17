package br.com.corely.finance.invoice.dto;

import java.math.BigDecimal;

public class DashboardResponse {

    private long pendingInvoices;
    private long paidInvoices;
    private long overdueInvoices;
    private long cancelledInvoices;
    private BigDecimal expectedRevenue;
    private BigDecimal receivedRevenue;
    private BigDecimal pendingRevenue;
    private long totalBilledStudents;

    public DashboardResponse() {}

    public DashboardResponse(long pendingInvoices, long paidInvoices, long overdueInvoices, long cancelledInvoices,
                              BigDecimal expectedRevenue, BigDecimal receivedRevenue, BigDecimal pendingRevenue,
                              long totalBilledStudents) {
        this.pendingInvoices = pendingInvoices;
        this.paidInvoices = paidInvoices;
        this.overdueInvoices = overdueInvoices;
        this.cancelledInvoices = cancelledInvoices;
        this.expectedRevenue = expectedRevenue;
        this.receivedRevenue = receivedRevenue;
        this.pendingRevenue = pendingRevenue;
        this.totalBilledStudents = totalBilledStudents;
    }

    public long getPendingInvoices() { return pendingInvoices; }
    public long getPaidInvoices() { return paidInvoices; }
    public long getOverdueInvoices() { return overdueInvoices; }
    public long getCancelledInvoices() { return cancelledInvoices; }
    public BigDecimal getExpectedRevenue() { return expectedRevenue; }
    public BigDecimal getReceivedRevenue() { return receivedRevenue; }
    public BigDecimal getPendingRevenue() { return pendingRevenue; }
    public long getTotalBilledStudents() { return totalBilledStudents; }
}
