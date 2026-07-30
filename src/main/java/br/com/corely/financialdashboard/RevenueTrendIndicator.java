package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.DelinquencyEvolutionItem;
import br.com.corely.financialdashboard.dto.MonthlyRevenueItem;
import br.com.corely.financialdashboard.dto.RevenueTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueTrendIndicator {

    private final FinancialDashboardInvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public RevenueTrendResponse calculate() {
        LocalDate now = LocalDate.now();
        String startReferenceMonth = now.minusMonths(11).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<MonthlyRevenueItem> monthlyRevenue = calculateMonthlyRevenue(startReferenceMonth);
        List<DelinquencyEvolutionItem> delinquencyEvolution = calculateDelinquencyEvolution(startReferenceMonth);
        BigDecimal monthlyGrowthRate = calculateMonthlyGrowthRate(monthlyRevenue);

        return RevenueTrendResponse.builder()
                .monthlyRevenue(monthlyRevenue)
                .monthlyGrowthRate(monthlyGrowthRate)
                .delinquencyEvolution(delinquencyEvolution)
                .build();
    }

    private List<MonthlyRevenueItem> calculateMonthlyRevenue(String startReferenceMonth) {
        List<Object[]> data = invoiceRepository.revenuePerMonth(startReferenceMonth);
        List<MonthlyRevenueItem> result = new ArrayList<>();

        for (Object[] row : data) {
            String month = (String) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            Long invoiceCount = (Long) row[2];

            result.add(MonthlyRevenueItem.builder()
                    .month(month)
                    .revenue(revenue)
                    .invoiceCount(invoiceCount)
                    .build());
        }

        return result;
    }

    private List<DelinquencyEvolutionItem> calculateDelinquencyEvolution(String startReferenceMonth) {
        List<Object[]> overdueData = invoiceRepository.delinquencyPerMonth(startReferenceMonth);
        List<Object[]> totalData = invoiceRepository.revenuePerMonth(startReferenceMonth);

        var totalMap = new java.util.LinkedHashMap<String, Long>();
        for (Object[] row : totalData) {
            String month = (String) row[0];
            Long count = (Long) row[2];
            totalMap.put(month, count);
        }

        List<DelinquencyEvolutionItem> result = new ArrayList<>();
        for (Object[] row : overdueData) {
            String month = (String) row[0];
            Long overdueCount = (Long) row[1];
            Long totalCount = totalMap.getOrDefault(month, 0L);

            BigDecimal rate = totalCount > 0
                    ? BigDecimal.valueOf(overdueCount)
                            .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(DelinquencyEvolutionItem.builder()
                    .month(month)
                    .overdueCount(overdueCount)
                    .totalCount(totalCount)
                    .delinquencyRate(rate)
                    .build());
        }

        return result;
    }

    private BigDecimal calculateMonthlyGrowthRate(List<MonthlyRevenueItem> monthlyRevenue) {
        if (monthlyRevenue == null || monthlyRevenue.size() < 2) {
            return BigDecimal.ZERO;
        }

        MonthlyRevenueItem current = monthlyRevenue.get(monthlyRevenue.size() - 1);
        MonthlyRevenueItem previous = monthlyRevenue.get(monthlyRevenue.size() - 2);

        if (previous.getRevenue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return current.getRevenue()
                .subtract(previous.getRevenue())
                .divide(previous.getRevenue(), 4, RoundingMode.HALF_UP);
    }
}
