package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialIndicatorsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialIndicatorsCalculatorTest {

    @Mock
    private FinancialDashboardInvoiceRepository invoiceRepository;

    @Mock
    private FinancialDashboardPaymentRepository paymentRepository;

    @InjectMocks
    private FinancialIndicatorsCalculator calculator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectIndicators() {
        when(invoiceRepository.countInvoicesForMonth(referenceMonth)).thenReturn(75L);
        when(invoiceRepository.countOverdueForMonth(referenceMonth)).thenReturn(5L);
        when(paymentRepository.countAndSumPayments(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)))
                .thenReturn(new Object[]{60L, new BigDecimal("14400.00")});
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("18000.00"));

        Object[] paidRow = new Object[]{"PAID", 60L, new BigDecimal("14400.00")};
        Object[] pendingRow = new Object[]{"PENDING", 10L, new BigDecimal("2400.00")};
        Object[] overdueRow = new Object[]{"OVERDUE", 5L, new BigDecimal("1200.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
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
        when(invoiceRepository.countInvoicesForMonth(referenceMonth)).thenReturn(0L);
        when(invoiceRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(paymentRepository.countAndSumPayments(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)))
                .thenReturn(new Object[]{0L, BigDecimal.ZERO});
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
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
        when(invoiceRepository.countInvoicesForMonth(referenceMonth)).thenReturn(100L);
        when(invoiceRepository.countOverdueForMonth(referenceMonth)).thenReturn(30L);
        when(paymentRepository.countAndSumPayments(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)))
                .thenReturn(new Object[]{50L, new BigDecimal("10000.00")});
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("25000.00"));

        Object[] paidRow = new Object[]{"PAID", 50L, new BigDecimal("10000.00")};
        Object[] overdueRow = new Object[]{"OVERDUE", 30L, new BigDecimal("7500.00")};
        Object[] pendingRow = new Object[]{"PENDING", 20L, new BigDecimal("5000.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, overdueRow, pendingRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getDelinquencyRate()).isEqualByComparingTo(new BigDecimal("0.3000"));
        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(new BigDecimal("0.4000"));
    }

    @Test
    void calculate_allCancelled_shouldReturnCorrectPercentages() {
        when(invoiceRepository.countInvoicesForMonth(referenceMonth)).thenReturn(10L);
        when(invoiceRepository.countOverdueForMonth(referenceMonth)).thenReturn(0L);
        when(paymentRepository.countAndSumPayments(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)))
                .thenReturn(new Object[]{0L, BigDecimal.ZERO});
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("3000.00"));

        Object[] cancelledRow = new Object[]{"CANCELLED", 10L, new BigDecimal("3000.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(cancelledRow));

        FinancialIndicatorsResponse response = calculator.calculate(referenceMonth);

        assertThat(response.getReceivedPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCancelledPercentage()).isEqualByComparingTo(new BigDecimal("1.0000"));
        assertThat(response.getDelinquencyRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
