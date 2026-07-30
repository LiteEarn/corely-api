package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.DelinquencyEvolutionItem;
import br.com.corely.financialdashboard.dto.MonthlyRevenueItem;
import br.com.corely.financialdashboard.dto.RevenueTrendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueTrendIndicatorTest {

    @Mock
    private RevenueDashboardRepository revenueDashboardRepository;

    @InjectMocks
    private RevenueTrendIndicator indicator;

    @BeforeEach
    void setUp() {
    }

    @Test
    void calculate_shouldReturnCorrectTrend() {
        Object[] revenueRow1 = new Object[]{"2025-08", new BigDecimal("10000.00"), 45L};
        Object[] revenueRow2 = new Object[]{"2025-09", new BigDecimal("12000.00"), 50L};
        Object[] revenueRow3 = new Object[]{"2026-07", new BigDecimal("15000.00"), 65L};

        Object[] overdueRow1 = new Object[]{"2025-08", 3L};
        Object[] overdueRow2 = new Object[]{"2025-09", 2L};
        Object[] overdueRow3 = new Object[]{"2026-07", 5L};

        when(revenueDashboardRepository.revenuePerMonth(anyString()))
                .thenReturn(Arrays.asList(revenueRow1, revenueRow2, revenueRow3));
        when(revenueDashboardRepository.delinquencyPerMonth(anyString()))
                .thenReturn(Arrays.asList(overdueRow1, overdueRow2, overdueRow3));

        RevenueTrendResponse response = indicator.calculate();

        assertThat(response.getMonthlyRevenue()).hasSize(3);
        assertThat(response.getDelinquencyEvolution()).hasSize(3);

        MonthlyRevenueItem first = response.getMonthlyRevenue().get(0);
        assertThat(first.getMonth()).isEqualTo("2025-08");
        assertThat(first.getRevenue()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(first.getInvoiceCount()).isEqualTo(45L);

        DelinquencyEvolutionItem dellFirst = response.getDelinquencyEvolution().get(0);
        assertThat(dellFirst.getMonth()).isEqualTo("2025-08");
        assertThat(dellFirst.getOverdueCount()).isEqualTo(3L);
        assertThat(dellFirst.getTotalCount()).isEqualTo(45L);
        assertThat(dellFirst.getDelinquencyRate()).isEqualByComparingTo(new BigDecimal("0.0667"));
    }

    @Test
    void calculate_whenNoData_shouldReturnEmptyLists() {
        when(revenueDashboardRepository.revenuePerMonth(anyString()))
                .thenReturn(Collections.emptyList());
        when(revenueDashboardRepository.delinquencyPerMonth(anyString()))
                .thenReturn(Collections.emptyList());

        RevenueTrendResponse response = indicator.calculate();

        assertThat(response.getMonthlyRevenue()).isEmpty();
        assertThat(response.getMonthlyGrowthRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDelinquencyEvolution()).isEmpty();
    }

    @Test
    void calculate_monthlyGrowthRate_shouldCalculateCorrectly() {
        Object[] revenueRow1 = new Object[]{"2025-08", new BigDecimal("10000.00"), 45L};
        Object[] revenueRow2 = new Object[]{"2025-09", new BigDecimal("11000.00"), 50L};
        Object[] revenueRow3 = new Object[]{"2026-07", new BigDecimal("12100.00"), 55L};

        Object[] overdueRow = new Object[]{"2026-07", 2L};

        when(revenueDashboardRepository.revenuePerMonth(anyString()))
                .thenReturn(Arrays.asList(revenueRow1, revenueRow2, revenueRow3));
        when(revenueDashboardRepository.delinquencyPerMonth(anyString()))
                .thenReturn(Collections.singletonList(overdueRow));

        RevenueTrendResponse response = indicator.calculate();

        assertThat(response.getMonthlyGrowthRate()).isEqualByComparingTo(new BigDecimal("0.1000"));
    }

    @Test
    void calculate_whenOnlyOneMonth_shouldReturnZeroGrowth() {
        Object[] revenueRow = new Object[]{"2026-07", new BigDecimal("15000.00"), 65L};
        Object[] overdueRow = new Object[]{"2026-07", 5L};

        when(revenueDashboardRepository.revenuePerMonth(anyString()))
                .thenReturn(Collections.singletonList(revenueRow));
        when(revenueDashboardRepository.delinquencyPerMonth(anyString()))
                .thenReturn(Collections.singletonList(overdueRow));

        RevenueTrendResponse response = indicator.calculate();

        assertThat(response.getMonthlyGrowthRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenPreviousRevenueZero_shouldReturnZeroGrowth() {
        Object[] revenueRow1 = new Object[]{"2025-08", BigDecimal.ZERO, 0L};
        Object[] revenueRow2 = new Object[]{"2026-07", new BigDecimal("5000.00"), 20L};
        Object[] overdueRow = new Object[]{"2026-07", 1L};

        when(revenueDashboardRepository.revenuePerMonth(anyString()))
                .thenReturn(Arrays.asList(revenueRow1, revenueRow2));
        when(revenueDashboardRepository.delinquencyPerMonth(anyString()))
                .thenReturn(Collections.singletonList(overdueRow));

        RevenueTrendResponse response = indicator.calculate();

        assertThat(response.getMonthlyGrowthRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_whenOverdueWithoutTotal_shouldReturnZeroRate() {
        Object[] revenueRow = new Object[]{"2026-07", new BigDecimal("15000.00"), 65L};
        Object[] overdueRow = new Object[]{"2025-08", 3L};

        when(revenueDashboardRepository.revenuePerMonth(anyString()))
                .thenReturn(Collections.singletonList(revenueRow));
        when(revenueDashboardRepository.delinquencyPerMonth(anyString()))
                .thenReturn(Collections.singletonList(overdueRow));

        RevenueTrendResponse response = indicator.calculate();

        assertThat(response.getDelinquencyEvolution()).hasSize(1);
        DelinquencyEvolutionItem item = response.getDelinquencyEvolution().get(0);
        assertThat(item.getMonth()).isEqualTo("2025-08");
        assertThat(item.getOverdueCount()).isEqualTo(3L);
        assertThat(item.getTotalCount()).isEqualTo(0L);
        assertThat(item.getDelinquencyRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
