package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialIndicatorsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinancialIndicatorsCalculator {

    private final RevenueDashboardRepository revenueDashboardRepository;
    private final PaymentDashboardRepository paymentDashboardRepository;

    @Transactional(readOnly = true)
    public FinancialIndicatorsResponse calculate(String referenceMonth) {
        long totalInvoices = revenueDashboardRepository.countInvoicesForMonth(referenceMonth);
        long overdueInvoices = revenueDashboardRepository.countOverdueForMonth(referenceMonth);

        BigDecimal delinquencyRate = totalInvoices > 0
                ? BigDecimal.valueOf(overdueInvoices)
                        .divide(BigDecimal.valueOf(totalInvoices), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDate monthStart = LocalDate.parse(referenceMonth + "-01");
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        Object[] paymentData = paymentDashboardRepository.countAndSumPayments(monthStart, monthEnd);
        Long paymentCount = paymentData[0] != null ? ((Number) paymentData[0]).longValue() : 0L;
        BigDecimal paymentSum = paymentData[1] != null ? (BigDecimal) paymentData[1] : BigDecimal.ZERO;

        BigDecimal averageTicket = paymentCount > 0
                ? paymentSum.divide(BigDecimal.valueOf(paymentCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalInvoiced = revenueDashboardRepository.totalInvoicedForMonth(referenceMonth);

        BigDecimal receivedPercentage = totalInvoiced.compareTo(BigDecimal.ZERO) > 0
                ? paymentSum.divide(totalInvoiced, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Object[] statusData = calculateStatusPercentages(referenceMonth, totalInvoiced);

        return FinancialIndicatorsResponse.builder()
                .averageTicket(averageTicket)
                .delinquencyRate(delinquencyRate)
                .receivedPercentage((BigDecimal) statusData[0])
                .pendingPercentage((BigDecimal) statusData[1])
                .overduePercentage((BigDecimal) statusData[2])
                .cancelledPercentage((BigDecimal) statusData[3])
                .build();
    }

    private Object[] calculateStatusPercentages(String referenceMonth, BigDecimal totalInvoiced) {
        if (totalInvoiced.compareTo(BigDecimal.ZERO) <= 0) {
            return new Object[]{
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            };
        }

        var statusData = revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth);

        BigDecimal received = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        BigDecimal cancelled = BigDecimal.ZERO;

        for (Object[] row : statusData) {
            String status = row[0].toString();
            BigDecimal sum = (BigDecimal) row[2];
            switch (status) {
                case "PAID" -> received = sum;
                case "PENDING" -> pending = sum;
                case "OVERDUE" -> overdue = sum;
                case "CANCELLED" -> cancelled = sum;
            }
        }

        return new Object[]{
                received.divide(totalInvoiced, 4, RoundingMode.HALF_UP),
                pending.divide(totalInvoiced, 4, RoundingMode.HALF_UP),
                overdue.divide(totalInvoiced, 4, RoundingMode.HALF_UP),
                cancelled.divide(totalInvoiced, 4, RoundingMode.HALF_UP)
        };
    }
}
