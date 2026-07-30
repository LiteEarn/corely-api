package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialDashboardServiceTest {

    @Mock
    private RevenueSummaryIndicator revenueSummaryIndicator;

    @Mock
    private StudentMetricsIndicator studentMetricsIndicator;

    @Mock
    private PlanMetricsIndicator planMetricsIndicator;

    @Mock
    private PaymentMetricsIndicator paymentMetricsIndicator;

    @Mock
    private FinancialIndicatorsCalculator financialIndicatorsCalculator;

    @Mock
    private RevenueTrendIndicator revenueTrendIndicator;

    @InjectMocks
    private FinancialDashboardService service;

    @Test
    void getDashboard_shouldAssembleAllIndicators() {
        RevenueSummaryResponse revenueSummary = new RevenueSummaryResponse();
        StudentMetricsResponse studentMetrics = new StudentMetricsResponse();
        PlanMetricsResponse planMetrics = new PlanMetricsResponse();
        PaymentMetricsResponse paymentMetrics = new PaymentMetricsResponse();
        FinancialIndicatorsResponse financialIndicators = new FinancialIndicatorsResponse();
        RevenueTrendResponse revenueTrend = new RevenueTrendResponse();

        when(revenueSummaryIndicator.calculate("2026-07")).thenReturn(revenueSummary);
        when(studentMetricsIndicator.calculate("2026-07")).thenReturn(studentMetrics);
        when(planMetricsIndicator.calculate("2026-07")).thenReturn(planMetrics);
        when(paymentMetricsIndicator.calculate("2026-07")).thenReturn(paymentMetrics);
        when(financialIndicatorsCalculator.calculate("2026-07")).thenReturn(financialIndicators);
        when(revenueTrendIndicator.calculate()).thenReturn(revenueTrend);

        FinancialDashboardResponse response = service.getDashboard("2026-07");

        assertThat(response.getRevenueSummary()).isSameAs(revenueSummary);
        assertThat(response.getStudentMetrics()).isSameAs(studentMetrics);
        assertThat(response.getPlanMetrics()).isSameAs(planMetrics);
        assertThat(response.getPaymentMetrics()).isSameAs(paymentMetrics);
        assertThat(response.getFinancialIndicators()).isSameAs(financialIndicators);
        assertThat(response.getRevenueTrend()).isSameAs(revenueTrend);

        verify(revenueSummaryIndicator).calculate("2026-07");
        verify(studentMetricsIndicator).calculate("2026-07");
        verify(planMetricsIndicator).calculate("2026-07");
        verify(paymentMetricsIndicator).calculate("2026-07");
        verify(financialIndicatorsCalculator).calculate("2026-07");
        verify(revenueTrendIndicator).calculate();
        verifyNoMoreInteractions(revenueSummaryIndicator, studentMetricsIndicator,
                planMetricsIndicator, paymentMetricsIndicator, financialIndicatorsCalculator,
                revenueTrendIndicator);
    }

    @Test
    void getDashboard_whenNullMonth_shouldDefaultToCurrentMonth() {
        String expectedMonth = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        when(revenueSummaryIndicator.calculate(expectedMonth)).thenReturn(new RevenueSummaryResponse());
        when(studentMetricsIndicator.calculate(expectedMonth)).thenReturn(new StudentMetricsResponse());
        when(planMetricsIndicator.calculate(expectedMonth)).thenReturn(new PlanMetricsResponse());
        when(paymentMetricsIndicator.calculate(expectedMonth)).thenReturn(new PaymentMetricsResponse());
        when(financialIndicatorsCalculator.calculate(expectedMonth)).thenReturn(new FinancialIndicatorsResponse());
        when(revenueTrendIndicator.calculate()).thenReturn(new RevenueTrendResponse());

        FinancialDashboardResponse response = service.getDashboard(null);

        assertThat(response).isNotNull();
        verify(revenueSummaryIndicator).calculate(expectedMonth);
    }
}