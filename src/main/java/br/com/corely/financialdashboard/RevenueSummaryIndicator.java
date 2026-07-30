package br.com.corely.financialdashboard;

import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.financialdashboard.dto.RevenueSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevenueSummaryIndicator {

    private final RevenueDashboardRepository revenueDashboardRepository;

    @Transactional(readOnly = true)
    public RevenueSummaryResponse calculate(String referenceMonth) {
        List<Object[]> statusData = revenueDashboardRepository.countAndSumInvoicesByStatusForMonth(referenceMonth);

        Map<InvoiceStatus, StatusAggregate> aggregates = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            aggregates.put(status, new StatusAggregate(0L, BigDecimal.ZERO));
        }

        for (Object[] row : statusData) {
            InvoiceStatus status = (InvoiceStatus) row[0];
            Long count = (Long) row[1];
            BigDecimal sum = (BigDecimal) row[2];
            aggregates.put(status, new StatusAggregate(count, sum));
        }

        StatusAggregate paid = aggregates.get(InvoiceStatus.PAID);
        StatusAggregate pending = aggregates.get(InvoiceStatus.PENDING);
        StatusAggregate overdue = aggregates.get(InvoiceStatus.OVERDUE);
        StatusAggregate cancelled = aggregates.get(InvoiceStatus.CANCELLED);

        BigDecimal totalInvoiced = revenueDashboardRepository.totalInvoicedForMonth(referenceMonth);
        Long totalInvoiceCount = revenueDashboardRepository.countInvoicesForMonth(referenceMonth);

        return RevenueSummaryResponse.builder()
                .receivedRevenue(paid.amount)
                .pendingRevenue(pending.amount)
                .overdueRevenue(overdue.amount)
                .cancelledRevenue(cancelled.amount)
                .expectedRevenue(paid.amount.add(pending.amount).add(overdue.amount))
                .totalInvoiced(totalInvoiced)
                .totalInvoiceCount(totalInvoiceCount)
                .paidInvoiceCount(paid.count)
                .pendingInvoiceCount(pending.count)
                .overdueInvoiceCount(overdue.count)
                .cancelledInvoiceCount(cancelled.count)
                .build();
    }

    private record StatusAggregate(Long count, BigDecimal amount) {}
}
