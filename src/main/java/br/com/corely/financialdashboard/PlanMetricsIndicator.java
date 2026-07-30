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

@Service
@RequiredArgsConstructor
public class PlanMetricsIndicator {

    private final FinancialDashboardInvoiceRepository invoiceRepository;
    private final FinancialDashboardStudentPlanRepository studentPlanRepository;

    @Transactional(readOnly = true)
    public PlanMetricsResponse calculate(String referenceMonth) {
        List<Object[]> revenueData = invoiceRepository.revenuePerPlanForMonth(referenceMonth);
        List<Object[]> countData = studentPlanRepository.countActivePerPlan();

        Map<String, PlanAggregate> planMap = new LinkedHashMap<>();

        for (Object[] row : countData) {
            String planName = (String) row[0];
            BigDecimal planPrice = (BigDecimal) row[1];
            Long studentCount = (Long) row[2];
            planMap.put(planName, new PlanAggregate(planName, planPrice, BigDecimal.ZERO, studentCount));
        }

        for (Object[] row : revenueData) {
            String planName = (String) row[0];
            BigDecimal planPrice = (BigDecimal) row[1];
            BigDecimal revenue = (BigDecimal) row[3];
            planMap.merge(planName, new PlanAggregate(planName, planPrice, revenue, 0L),
                    (existing, incoming) -> new PlanAggregate(planName, existing.planPrice, revenue, existing.studentCount));
        }

        List<PlanRevenueItem> plans = new ArrayList<>();
        for (PlanAggregate agg : planMap.values()) {
            BigDecimal averageTicket = agg.studentCount > 0
                    ? agg.revenue.divide(BigDecimal.valueOf(agg.studentCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            plans.add(PlanRevenueItem.builder()
                    .planName(agg.planName)
                    .planPrice(agg.planPrice)
                    .revenue(agg.revenue)
                    .studentCount(agg.studentCount)
                    .averageTicket(averageTicket)
                    .build());
        }

        return PlanMetricsResponse.builder()
                .plans(plans)
                .totalActivePlans((long) plans.size())
                .build();
    }

    private record PlanAggregate(String planName, BigDecimal planPrice, BigDecimal revenue, long studentCount) {}
}
