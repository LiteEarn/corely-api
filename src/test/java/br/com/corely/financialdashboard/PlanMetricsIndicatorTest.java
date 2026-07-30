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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanMetricsIndicatorTest {

    @Mock
    private RevenueDashboardRepository revenueDashboardRepository;

    @Mock
    private StudentDashboardRepository studentDashboardRepository;

    @InjectMocks
    private PlanMetricsIndicator indicator;

    private String referenceMonth;

    private final UUID premiumId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID basicId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID goldId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private final UUID silverId = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectPlanMetrics() {
        Object[] premiumRevenue = new Object[]{premiumId, "Premium", new BigDecimal("299.00"), 40L, new BigDecimal("11960.00")};
        Object[] basicRevenue = new Object[]{basicId, "Básico", new BigDecimal("99.00"), 60L, new BigDecimal("5940.00")};

        Object[] premiumCount = new Object[]{premiumId, "Premium", new BigDecimal("299.00"), 45L};
        Object[] basicCount = new Object[]{basicId, "Básico", new BigDecimal("99.00"), 65L};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Arrays.asList(premiumRevenue, basicRevenue));
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Arrays.asList(premiumCount, basicCount));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(2);
        assertThat(response.getTotalActivePlans()).isEqualTo(2L);

        PlanRevenueItem premium = response.getPlans().get(0);
        assertThat(premium.getPlanName()).isEqualTo("Premium");
        assertThat(premium.getPlanPrice()).isEqualByComparingTo(new BigDecimal("299.00"));
        assertThat(premium.getRevenue()).isEqualByComparingTo(new BigDecimal("11960.00"));
        assertThat(premium.getPaidInvoiceCount()).isEqualTo(40L);
        assertThat(premium.getStudentCount()).isEqualTo(45L);
        assertThat(premium.getAverageTicket()).isEqualByComparingTo(new BigDecimal("299.00"));
    }

    @Test
    void calculate_whenNoRevenueData_shouldReturnZeroRevenue() {
        Object[] premiumCount = new Object[]{premiumId, "Premium", new BigDecimal("299.00"), 10L};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.emptyList());
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Collections.singletonList(premiumCount));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(1);
        PlanRevenueItem premium = response.getPlans().get(0);
        assertThat(premium.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(premium.getPaidInvoiceCount()).isEqualTo(0L);
        assertThat(premium.getStudentCount()).isEqualTo(10L);
        assertThat(premium.getAverageTicket()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenNoActivePlans_shouldReturnEmptyList() {
        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.emptyList());
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Collections.emptyList());

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).isEmpty();
        assertThat(response.getTotalActivePlans()).isEqualTo(0L);
    }

    @Test
    void calculate_withMultiplePlans_shouldCalculateCorrectAverages() {
        Object[] plan1Revenue = new Object[]{goldId, "Gold", new BigDecimal("199.00"), 20L, new BigDecimal("3980.00")};
        Object[] plan2Revenue = new Object[]{silverId, "Silver", new BigDecimal("149.00"), 15L, new BigDecimal("2235.00")};

        Object[] plan1Count = new Object[]{goldId, "Gold", new BigDecimal("199.00"), 25L};
        Object[] plan2Count = new Object[]{silverId, "Silver", new BigDecimal("149.00"), 20L};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Arrays.asList(plan1Revenue, plan2Revenue));
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Arrays.asList(plan1Count, plan2Count));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(2);

        PlanRevenueItem gold = response.getPlans().get(0);
        assertThat(gold.getAverageTicket()).isEqualByComparingTo(new BigDecimal("199.00"));

        PlanRevenueItem silver = response.getPlans().get(1);
        assertThat(silver.getAverageTicket()).isEqualByComparingTo(new BigDecimal("149.00"));
    }

    @Test
    void calculate_whenOnlyRevenueData_shouldUseRevenueData() {
        Object[] premiumRevenue = new Object[]{premiumId, "Premium", new BigDecimal("299.00"), 30L, new BigDecimal("8970.00")};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(premiumRevenue));
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Collections.emptyList());

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(1);
        PlanRevenueItem premium = response.getPlans().get(0);
        assertThat(premium.getPlanName()).isEqualTo("Premium");
        assertThat(premium.getRevenue()).isEqualByComparingTo(new BigDecimal("8970.00"));
        assertThat(premium.getPaidInvoiceCount()).isEqualTo(30L);
        assertThat(premium.getStudentCount()).isEqualTo(0L);
        assertThat(premium.getAverageTicket()).isEqualByComparingTo(new BigDecimal("299.00"));
    }

    @Test
    void calculate_whenDivisionByZero_shouldReturnZeroAverage() {
        Object[] premiumRevenue = new Object[]{premiumId, "Premium", new BigDecimal("299.00"), 0L, BigDecimal.ZERO};

        Object[] premiumCount = new Object[]{premiumId, "Premium", new BigDecimal("299.00"), 5L};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(premiumRevenue));
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Collections.singletonList(premiumCount));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(1);
        PlanRevenueItem premium = response.getPlans().get(0);
        assertThat(premium.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(premium.getPaidInvoiceCount()).isEqualTo(0L);
        assertThat(premium.getAverageTicket()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenSamePlanNameDifferentIds_shouldKeepSeparate() {
        UUID planA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID planB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

        Object[] revenueA = new Object[]{planA, "Plano X", new BigDecimal("100.00"), 10L, new BigDecimal("1000.00")};
        Object[] revenueB = new Object[]{planB, "Plano X", new BigDecimal("200.00"), 5L, new BigDecimal("1000.00")};

        Object[] countA = new Object[]{planA, "Plano X", new BigDecimal("100.00"), 15L};
        Object[] countB = new Object[]{planB, "Plano X", new BigDecimal("200.00"), 8L};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Arrays.asList(revenueA, revenueB));
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Arrays.asList(countA, countB));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getPlans()).hasSize(2);
        assertThat(response.getTotalActivePlans()).isEqualTo(2L);

        PlanRevenueItem first = response.getPlans().get(0);
        PlanRevenueItem second = response.getPlans().get(1);
        assertThat(first.getPlanName()).isEqualTo("Plano X");
        assertThat(second.getPlanName()).isEqualTo("Plano X");
        assertThat(first.getPlanPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(second.getPlanPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void calculate_withVaryingInvoiceAmounts_shouldCalculateAverageCorrectly() {
        UUID planId = UUID.fromString("00000000-0000-0000-0000-00000000000c");

        Object[] revenue = new Object[]{planId, "Variável", new BigDecimal("150.00"), 4L, new BigDecimal("750.00")};
        Object[] count = new Object[]{planId, "Variável", new BigDecimal("150.00"), 10L};

        when(revenueDashboardRepository.revenuePerPlanForMonth(referenceMonth))
                .thenReturn(Collections.singletonList(revenue));
        when(studentDashboardRepository.countActivePerPlan())
                .thenReturn(Collections.singletonList(count));

        PlanMetricsResponse response = indicator.calculate(referenceMonth);

        PlanRevenueItem item = response.getPlans().get(0);
        assertThat(item.getRevenue()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(item.getPaidInvoiceCount()).isEqualTo(4L);
        assertThat(item.getStudentCount()).isEqualTo(10L);
        assertThat(item.getAverageTicket()).isEqualByComparingTo(new BigDecimal("187.50"));
    }
}
