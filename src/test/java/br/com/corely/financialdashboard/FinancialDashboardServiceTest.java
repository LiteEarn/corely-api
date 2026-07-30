package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

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
        when(revenueTrendIndicator.calculate("2026-07")).thenReturn(revenueTrend);

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
        verify(revenueTrendIndicator).calculate("2026-07");
        verifyNoMoreInteractions(revenueSummaryIndicator, studentMetricsIndicator,
                planMetricsIndicator, paymentMetricsIndicator, financialIndicatorsCalculator,
                revenueTrendIndicator);
    }

    @Test
    void getDashboard_whenNullMonth_shouldDefaultToCurrentMonth() {
        String expectedMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        when(revenueSummaryIndicator.calculate(expectedMonth)).thenReturn(new RevenueSummaryResponse());
        when(studentMetricsIndicator.calculate(expectedMonth)).thenReturn(new StudentMetricsResponse());
        when(planMetricsIndicator.calculate(expectedMonth)).thenReturn(new PlanMetricsResponse());
        when(paymentMetricsIndicator.calculate(expectedMonth)).thenReturn(new PaymentMetricsResponse());
        when(financialIndicatorsCalculator.calculate(expectedMonth)).thenReturn(new FinancialIndicatorsResponse());
        when(revenueTrendIndicator.calculate(expectedMonth)).thenReturn(new RevenueTrendResponse());

        FinancialDashboardResponse response = service.getDashboard(null);

        assertThat(response).isNotNull();
        verify(revenueSummaryIndicator).calculate(expectedMonth);
        verify(revenueTrendIndicator).calculate(expectedMonth);
        verifyNoMoreInteractions(revenueTrendIndicator);
    }

    @Test
    void getDashboard_withHistoricalMonth_shouldPassSameMonthToAllIndicators() {
        String historicalMonth = "2025-03";

        when(revenueSummaryIndicator.calculate(historicalMonth)).thenReturn(new RevenueSummaryResponse());
        when(studentMetricsIndicator.calculate(historicalMonth)).thenReturn(new StudentMetricsResponse());
        when(planMetricsIndicator.calculate(historicalMonth)).thenReturn(new PlanMetricsResponse());
        when(paymentMetricsIndicator.calculate(historicalMonth)).thenReturn(new PaymentMetricsResponse());
        when(financialIndicatorsCalculator.calculate(historicalMonth)).thenReturn(new FinancialIndicatorsResponse());
        when(revenueTrendIndicator.calculate(historicalMonth)).thenReturn(new RevenueTrendResponse());

        FinancialDashboardResponse response = service.getDashboard(historicalMonth);

        assertThat(response).isNotNull();
        verify(revenueTrendIndicator).calculate(historicalMonth);
        verify(revenueSummaryIndicator).calculate(historicalMonth);
        verify(studentMetricsIndicator).calculate(historicalMonth);
        verify(planMetricsIndicator).calculate(historicalMonth);
        verify(paymentMetricsIndicator).calculate(historicalMonth);
        verify(financialIndicatorsCalculator).calculate(historicalMonth);
    }
}