package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
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
    public FinancialDashboardResponse getDashboard(String referenceMonth) {
        if (referenceMonth == null) {
            referenceMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        return FinancialDashboardResponse.builder()
                .revenueSummary(revenueSummaryIndicator.calculate(referenceMonth))
                .studentMetrics(studentMetricsIndicator.calculate(referenceMonth))
                .planMetrics(planMetricsIndicator.calculate(referenceMonth))
                .paymentMetrics(paymentMetricsIndicator.calculate(referenceMonth))
                .financialIndicators(financialIndicatorsCalculator.calculate(referenceMonth))
                .revenueTrend(revenueTrendIndicator.calculate())
                .build();
    }
}
