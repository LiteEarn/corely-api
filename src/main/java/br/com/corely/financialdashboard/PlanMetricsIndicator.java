package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.PlanMetricsResponse;
import br.com.corely.financialdashboard.dto.PlanRevenueItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanMetricsIndicator {

    private final RevenueDashboardRepository revenueDashboardRepository;
    private final StudentDashboardRepository studentDashboardRepository;

    @Transactional(readOnly = true)
    public PlanMetricsResponse calculate(String referenceMonth) {
        List<Object[]> revenueData = revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth);
        List<Object[]> countData = studentDashboardRepository.countActivePerPlan();

        Map<UUID, PlanAggregate> planMap = new LinkedHashMap<>();

        for (Object[] row : countData) {
            UUID csId = (UUID) row[0];
            String planName = (String) row[1];
            BigDecimal planPrice = (BigDecimal) row[2];
            Long studentCount = (Long) row[3];
            planMap.put(csId, new PlanAggregate(csId, planName, planPrice, BigDecimal.ZERO, 0L, studentCount));
        }

        for (Object[] row : revenueData) {
            UUID csId = (UUID) row[0];
            String planName = (String) row[1];
            BigDecimal planPrice = (BigDecimal) row[2];
            Long paidInvoiceCount = (Long) row[3];
            BigDecimal revenue = (BigDecimal) row[4];

            PlanAggregate existing = planMap.get(csId);
            if (existing != null) {
                planMap.put(csId, new PlanAggregate(csId, planName, planPrice, revenue, paidInvoiceCount, existing.studentCount));
            } else {
                planMap.put(csId, new PlanAggregate(csId, planName, planPrice, revenue, paidInvoiceCount, 0L));
            }
        }

        List<PlanRevenueItem> plans = new ArrayList<>();
        for (PlanAggregate agg : planMap.values()) {
            BigDecimal averageTicket = agg.paidInvoiceCount > 0
                    ? agg.revenue.divide(BigDecimal.valueOf(agg.paidInvoiceCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            plans.add(PlanRevenueItem.builder()
                    .planName(agg.planName)
                    .planPrice(agg.planPrice)
                    .revenue(agg.revenue)
                    .paidInvoiceCount(agg.paidInvoiceCount)
                    .studentCount(agg.studentCount)
                    .averageTicket(averageTicket)
                    .build());
        }

        return PlanMetricsResponse.builder()
                .plans(plans)
                .totalActivePlans((long) plans.size())
                .build();
    }

    private record PlanAggregate(UUID contractSnapshotId, String planName, BigDecimal planPrice, BigDecimal revenue, long paidInvoiceCount, long studentCount) {}
}
