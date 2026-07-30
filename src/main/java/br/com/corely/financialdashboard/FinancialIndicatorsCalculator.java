package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialIndicatorsResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialIndicatorsCalculator {

    private static final Logger log = LoggerFactory.getLogger(FinancialIndicatorsCalculator.class);

    private final RevenueDashboardRepository revenueDashboardRepository;

    @Transactional(readOnly = true)
    public FinancialIndicatorsResponse calculate(String referenceMonth) {
        long totalInvoices = revenueDashboardRepository.countInvoicesForMonth(referenceMonth);
        long overdueInvoices = revenueDashboardRepository.countOverdueForMonth(referenceMonth);

        BigDecimal delinquencyRate = totalInvoices > 0
                ? BigDecimal.valueOf(overdueInvoices)
                        .divide(BigDecimal.valueOf(totalInvoices), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalInvoiced = revenueDashboardRepository.totalInvoicedForMonth(referenceMonth);

        List<Object[]> statusData = revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth);
        StatusAmounts amounts = extractStatusAmounts(statusData);

        BigDecimal averageTicket = amounts.paidCount > 0
                ? amounts.paidSum.divide(BigDecimal.valueOf(amounts.paidCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        boolean hasInvoicing = totalInvoiced.compareTo(BigDecimal.ZERO) > 0;
        StatusPercentages pct = hasInvoicing
                ? calculatePercentages(amounts, totalInvoiced)
                : new StatusPercentages(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        return FinancialIndicatorsResponse.builder()
                .averageTicket(averageTicket)
                .delinquencyRate(delinquencyRate)
                .receivedPercentage(pct.received)
                .pendingPercentage(pct.pending)
                .overduePercentage(pct.overdue)
                .cancelledPercentage(pct.cancelled)
                .build();
    }

    private StatusAmounts extractStatusAmounts(List<Object[]> statusData) {
        BigDecimal paidSum = BigDecimal.ZERO;
        long paidCount = 0L;
        BigDecimal pendingSum = BigDecimal.ZERO;
        BigDecimal overdueSum = BigDecimal.ZERO;
        BigDecimal cancelledSum = BigDecimal.ZERO;

        for (Object[] row : statusData) {
            String status = row[0].toString();
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal sum = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

            switch (status) {
                case "PAID" -> { paidSum = sum; paidCount = count; }
                case "PENDING" -> pendingSum = sum;
                case "OVERDUE" -> overdueSum = sum;
                case "CANCELLED" -> cancelledSum = sum;
                default -> log.warn("Unknown invoice status '{}' for month", status);
            }
        }

        return new StatusAmounts(paidSum, paidCount, pendingSum, overdueSum, cancelledSum);
    }

    private StatusPercentages calculatePercentages(StatusAmounts amounts, BigDecimal totalInvoiced) {
        return new StatusPercentages(
                amounts.paidSum.divide(totalInvoiced, 4, RoundingMode.HALF_UP),
                amounts.pendingSum.divide(totalInvoiced, 4, RoundingMode.HALF_UP),
                amounts.overdueSum.divide(totalInvoiced, 4, RoundingMode.HALF_UP),
                amounts.cancelledSum.divide(totalInvoiced, 4, RoundingMode.HALF_UP)
        );
    }

    private record StatusAmounts(BigDecimal paidSum, long paidCount, BigDecimal pendingSum, BigDecimal overdueSum, BigDecimal cancelledSum) {}

    private record StatusPercentages(BigDecimal received, BigDecimal pending, BigDecimal overdue, BigDecimal cancelled) {}
}
