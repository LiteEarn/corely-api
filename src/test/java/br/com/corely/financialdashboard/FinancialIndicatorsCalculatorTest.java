package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialIndicatorsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialIndicatorsCalculatorTest {

    @Mock
    private RevenueDashboardRepository revenueDashboardRepository;

    @InjectMocks
    private FinancialIndicatorsCalculator calculator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectIndicators() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(75L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(5L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("18000.00"));

        Object[] paidRow = new Object[]{"PAID", 60L, new BigDecimal("14400.00")};
        Object[] pendingRow = new Object[]{"PENDING", 10L, new BigDecimal("2400.00")};
        Object[] overdueRow = new Object[]{"OVERDUE", 5L, new BigDecimal("1200.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, pendingRow, overdueRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getAverageTicket()).isEqualByComparingTo(new BigDecimal("240.00"));
        assertThat(response.getDelinquencyRate()).isEqualByComparingTo(new BigDecimal("0.0667"));
        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(new BigDecimal("0.8000"));
        assertThat(response.getPendingPercentage()).isEqualByComparingTo(new BigDecimal("0.1333"));
        assertThat(response.getOverduePercentage()).isEqualByComparingTo(new BigDecimal("0.0667"));
    }

    @Test
    void calculate_whenNoData_shouldReturnZeros() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(BigDecimal.ZERO);

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getAverageTicket()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDelinquencyRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPendingPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOverduePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_withHighDelinquency_shouldReturnCorrectRate() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(100L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(30L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("25000.00"));

        Object[] paidRow = new Object[]{"PAID", 50L, new BigDecimal("10000.00")};
        Object[] overdueRow = new Object[]{"OVERDUE", 30L, new BigDecimal("7500.00")};
        Object[] pendingRow = new Object[]{"PENDING", 20L, new BigDecimal("5000.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, overdueRow, pendingRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getDelinquencyRate()).isEqualByComparingTo(new BigDecimal("0.3000"));
        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(new BigDecimal("0.4000"));
    }

    @Test
    void calculate_allCancelled_shouldReturnCorrectPercentages() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(10L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("3000.00"));

        Object[] cancelledRow = new Object[]{"CANCELLED", 10L, new BigDecimal("3000.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(cancelledRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCancelledPercentage()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(response.getDelinquencyRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenOnlyPaidInvoices_shouldReturnFullReceived() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(50L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("10000.00"));

        Object[] paidRow = new Object[]{"PAID", 50L, new BigDecimal("10000.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(paidRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(response.getAverageTicket()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getPendingPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOverduePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCancelledPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenCancelledHasValue_shouldNotAffectReceived() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(60L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("12000.00"));

        Object[] paidRow = new Object[]{"PAID", 40L, new BigDecimal("8000.00")};
        Object[] cancelledRow = new Object[]{"CANCELLED", 20L, new BigDecimal("4000.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, cancelledRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(new BigDecimal("0.6667"));
        assertThat(response.getCancelledPercentage()).isEqualByComparingTo(new BigDecimal("0.3333"));
    }

    @Test
    void calculate_whenUnknownStatus_shouldIgnore() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(50L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("10000.00"));

        Object[] paidRow = new Object[]{"PAID", 30L, new BigDecimal("6000.00")};
        Object[] unknownRow = new Object[]{"REFUNDED", 20L, new BigDecimal("4000.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, unknownRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(new BigDecimal("0.6000"));
    }

    @Test
    void calculate_averageTicket_basedOnPaidInvoicesOnly() {
        when(revenueDashboardRepository.countInvoicesForMonth(referenceMonth)).thenReturn(10L);
        when(revenueDashboardRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(revenueDashboardRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("6000.00"));

        Object[] paidRow = new Object[]{"PAID", 10L, new BigDecimal("6000.00")};

        when(revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(paidRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getAverageTicket()).isEqualByComparingTo(new BigDecimal("600.00"));
    }
}
