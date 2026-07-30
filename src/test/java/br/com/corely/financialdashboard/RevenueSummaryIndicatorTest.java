package br.com.corely.financialdashboard;

import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.financialdashboard.dto.RevenueSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueSummaryIndicatorTest {

    @Mock
    private FinancialDashboardInvoiceRepository invoiceRepository;

    @InjectMocks
    private RevenueSummaryIndicator indicator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectRevenueSummary() {
        Object[] paidRow = new Object[]{InvoiceStatus.PAID, 50L, new BigDecimal("12000.00")};
        Object[] pendingRow = new Object[]{InvoiceStatus.PENDING, 10L, new BigDecimal("2000.00")};
        Object[] overdueRow = new Object[]{InvoiceStatus.OVERDUE, 4L, new BigDecimal("800.00")};
        Object[] cancelledRow = new Object[]{InvoiceStatus.CANCELLED, 1L, new BigDecimal("200.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, pendingRow, overdueRow, cancelledRow));
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("15000.00"));
        when(invoiceRepository.countInvoicesForMonth(referenceMonth))
                .thenReturn(65L);

        RevenueSummaryResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getReceivedRevenue()).isEqualByComparingTo(new BigDecimal("12000.00"));
        assertThat(response.getPendingRevenue()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(response.getOverdueRevenue()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(response.getCancelledRevenue()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getExpectedRevenue()).isEqualByComparingTo(new BigDecimal("14800.00"));
        assertThat(response.getTotalInvoiced()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(response.getTotalInvoiceCount()).isEqualTo(65L);
        assertThat(response.getPaidInvoiceCount()).isEqualTo(50L);
        assertThat(response.getPendingInvoiceCount()).isEqualTo(10L);
        assertThat(response.getOverdueInvoiceCount()).isEqualTo(4L);
        assertThat(response.getCancelledInvoiceCount()).isEqualTo(1L);
    }

    @Test
    void calculate_whenNoData_shouldReturnZeros() {
        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Collections.emptyList());
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.countInvoicesForMonth(referenceMonth))
                .thenReturn(0L);

        RevenueSummaryResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getReceivedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPendingRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOverdueRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCancelledRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getExpectedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalInvoiced()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalInvoiceCount()).isEqualTo(0L);
    }

    @Test
    void calculate_withOnlyPaidInvoices_shouldReturnCorrectValues() {
        Object[] paidRow = new Object[]{InvoiceStatus.PAID, 30L, new BigDecimal("9000.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(paidRow));
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("9000.00"));
        when(invoiceRepository.countInvoicesForMonth(referenceMonth))
                .thenReturn(30L);

        RevenueSummaryResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getReceivedRevenue()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(response.getPendingRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getOverdueRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCancelledRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getExpectedRevenue()).isEqualByComparingTo(new BigDecimal("9000.00"));
        assertThat(response.getPaidInvoiceCount()).isEqualTo(30L);
        assertThat(response.getPendingInvoiceCount()).isEqualTo(0L);
    }

    @Test
    void calculate_withMultiplePayments_shouldAggregateCorrectly() {
        Object[] paidRow = new Object[]{InvoiceStatus.PAID, 100L, new BigDecimal("25000.00")};
        Object[] pendingRow = new Object[]{InvoiceStatus.PENDING, 20L, new BigDecimal("5000.00")};
        Object[] overdueRow = new Object[]{InvoiceStatus.OVERDUE, 5L, new BigDecimal("1250.00")};
        Object[] cancelledRow = new Object[]{InvoiceStatus.CANCELLED, 2L, new BigDecimal("500.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, pendingRow, overdueRow, cancelledRow));
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("31750.00"));
        when(invoiceRepository.countInvoicesForMonth(referenceMonth))
                .thenReturn(127L);

        RevenueSummaryResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getExpectedRevenue()).isEqualByComparingTo(new BigDecimal("31250.00"));
        assertThat(response.getTotalInvoiced()).isEqualByComparingTo(new BigDecimal("31750.00"));
        assertThat(response.getTotalInvoiceCount()).isEqualTo(127L);
    }

    @Test
    void calculate_withCancelledInvoices_shouldIncludeInTotal() {
        Object[] paidRow = new Object[]{InvoiceStatus.PAID, 10L, new BigDecimal("3000.00")};
        Object[] cancelledRow = new Object[]{InvoiceStatus.CANCELLED, 5L, new BigDecimal("1500.00")};

        when(invoiceRepository.countAndSumInvoicesByStatusForMonth(referenceMonth))
                .thenReturn(Arrays.asList(paidRow, cancelledRow));
        when(invoiceRepository.totalInvoicedForMonth(referenceMonth))
                .thenReturn(new BigDecimal("4500.00"));
        when(invoiceRepository.countInvoicesForMonth(referenceMonth))
                .thenReturn(15L);

        RevenueSummaryResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getReceivedRevenue()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(response.getCancelledRevenue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(response.getExpectedRevenue()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(response.getTotalInvoiced()).isEqualByComparingTo(new BigDecimal("4500.00"));
    }
}
