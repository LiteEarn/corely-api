package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class FinancialDashboardService {

    private final RevenueSummaryIndicator revenueSummaryIndicator;
    private final StudentMetricsIndicator studentMetricsIndicator;
    private final PlanMetricsIndicator planMetricsIndicator;
    private final PaymentMetricsIndicator paymentMetricsIndicator;
    private final FinancialIndicatorsCalculator financialIndicatorsCalculator;
    private final RevenueTrendIndicator revenueTrendIndicator;

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getDashboard() {
        String referenceMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        var revenueSummary = revenueSummaryIndicator.calculate(referenceMonth);
        var studentMetrics = studentMetricsIndicator.calculate(referenceMonth);
        var planMetrics = planMetricsIndicator.calculate(referenceMonth);
        var paymentMetrics = paymentMetricsIndicator.calculate(referenceMonth);
        var financialIndicators = financialIndicatorsCalculator.calculate(referenceMonth);
        var revenueTrend = revenueTrendIndicator.calculate();

        return FinancialDashboardResponse.builder()
                .revenueSummary(revenueSummary)
                .studentMetrics(studentMetrics)
                .planMetrics(planMetrics)
                .paymentMetrics(paymentMetrics)
                .financialIndicators(financialIndicators)
                .revenueTrend(revenueTrend)
                .build();
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getDashboardForMonth(String referenceMonth) {
        var revenueSummary = revenueSummaryIndicator.calculate(referenceMonth);
        var studentMetrics = studentMetricsIndicator.calculate(referenceMonth);
        var planMetrics = planMetricsIndicator.calculate(referenceMonth);
        var paymentMetrics = paymentMetricsIndicator.calculate(referenceMonth);
        var financialIndicators = financialIndicatorsCalculator.calculate(referenceMonth);
        var revenueTrend = revenueTrendIndicator.calculate();

        return FinancialDashboardResponse.builder()
                .revenueSummary(revenueSummary)
                .studentMetrics(studentMetrics)
                .planMetrics(planMetrics)
                .paymentMetrics(paymentMetrics)
                .financialIndicators(financialIndicators)
                .revenueTrend(revenueTrend)
                .build();
    }
}
