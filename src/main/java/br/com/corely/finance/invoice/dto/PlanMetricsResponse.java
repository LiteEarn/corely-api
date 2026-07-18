package br.com.corely.finance.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PlanMetricsResponse {

    private UUID planId;
    private String planName;
    private long studentCount;
    private BigDecimal expectedMonthlyRevenue;

    public PlanMetricsResponse() {}

    public PlanMetricsResponse(UUID planId, String planName, long studentCount, BigDecimal expectedMonthlyRevenue) {
        this.planId = planId;
        this.planName = planName;
        this.studentCount = studentCount;
        this.expectedMonthlyRevenue = expectedMonthlyRevenue;
    }

    public UUID getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public long getStudentCount() { return studentCount; }
    public BigDecimal getExpectedMonthlyRevenue() { return expectedMonthlyRevenue; }
}
