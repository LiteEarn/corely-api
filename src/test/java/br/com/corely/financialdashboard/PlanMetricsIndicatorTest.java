package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.PlanMetricsResponse;
import br.com.corely.financialdashboard.dto.PlanRevenueItem;
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
class PlanMetricsIndicatorTest {

    @Mock
    private FinancialDashboardInvoiceRepository invoiceRepository;

    @Mock
    private FinancialDashboardStudentPlanRepository studentPlanRepository;

    @InjectMocks
    private PlanMetricsIndicator indicator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectPlanMetrics() {
        Object[] premiumRevenue = new Object[]{"Premium", new BigDecimal("299.00"), 40L, new BigDecimal("11960.00")};
        Object[] basicRevenue = new Object[]{"Básico", new BigDecimal("99.00"), 60L, new BigDecimal("5940.00")};

        Object[] premiumCount = new Object[]{"Premium", new BigDecimal("299.00"), 45L};
        Object[] basicCount = new Object[]{"Básico", new BigDecimal("99.00"), 65L};

        when(invoiceRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Arrays.asList(premiumRevenue, basicRevenue));
        when(studentPlanRepository.countActivePerPlan())
                .thenReturn(Arrays.asList(premiumCount, basicCount));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(2);
        assertThat(response.getTotalActivePlans()).isEqualTo(2L);

        PlanRevenueItem premium = response.getPlans().get(0);
        assertThat(premium.getPlanName()).isEqualTo("Premium");
        assertThat(premium.getPlanPrice()).isEqualByComparingTo(new BigDecimal("299.00"));
        assertThat(premium.getRevenue()).isEqualByComparingTo(new BigDecimal("11960.00"));
        assertThat(premium.getStudentCount()).isEqualTo(45L);
        assertThat(premium.getAverageTicket()).isEqualByComparingTo(new BigDecimal("265.78"));
    }

    @Test
    void calculate_whenNoRevenueData_shouldReturnZeroRevenue() {
        Object[] premiumCount = new Object[]{"Premium", new BigDecimal("299.00"), 10L};

        when(invoiceRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.emptyList());
        when(studentPlanRepository.countActivePerPlan())
                .thenReturn(Collections.singletonList(premiumCount));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(1);
        PlanRevenueItem premium = response.getPlans().get(0);
        assertThat(premium.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(premium.getStudentCount()).isEqualTo(10L);
        assertThat(premium.getAverageTicket()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenNoActivePlans_shouldReturnEmptyList() {
        when(invoiceRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.emptyList());
        when(studentPlanRepository.countActivePerPlan())
                .thenReturn(Collections.emptyList());

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).isEmpty();
        assertThat(response.getTotalActivePlans()).isEqualTo(0L);
    }

    @Test
    void calculate_withMultiplePlans_shouldCalculateCorrectAverages() {
        Object[] plan1Revenue = new Object[]{"Gold", new BigDecimal("199.00"), 20L, new BigDecimal("3980.00")};
        Object[] plan2Revenue = new Object[]{"Silver", new BigDecimal("149.00"), 15L, new BigDecimal("2235.00")};

        Object[] plan1Count = new Object[]{"Gold", new BigDecimal("199.00"), 25L};
        Object[] plan2Count = new Object[]{"Silver", new BigDecimal("149.00"), 20L};

        when(invoiceRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Arrays.asList(plan1Revenue, plan2Revenue));
        when(studentPlanRepository.countActivePerPlan())
                .thenReturn(Arrays.asList(plan1Count, plan2Count));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(2);

        PlanRevenueItem gold = response.getPlans().get(0);
        assertThat(gold.getAverageTicket()).isEqualByComparingTo(new BigDecimal("159.20"));

        PlanRevenueItem silver = response.getPlans().get(1);
        assertThat(silver.getAverageTicket()).isEqualByComparingTo(new BigDecimal("111.75"));
    }
}
