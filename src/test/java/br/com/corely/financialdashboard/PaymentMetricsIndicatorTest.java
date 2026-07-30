package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.PaymentDayResponse;
import br.com.corely.financialdashboard.dto.PaymentMetricsResponse;
import br.com.corely.financialdashboard.dto.PaymentMonthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMetricsIndicatorTest {

    @Mock
    private PaymentDashboardRepository paymentDashboardRepository;

    @InjectMocks
    private PaymentMetricsIndicator indicator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectPaymentMetrics() {
        LocalDate day1 = LocalDate.of(2026, 7, 10);
        LocalDate day2 = LocalDate.of(2026, 7, 15);
        Object[] dayRow1 = new Object[]{day1, 3L, new BigDecimal("750.00")};
        Object[] dayRow2 = new Object[]{day2, 5L, new BigDecimal("1250.00")};

        Object[] monthRow = new Object[]{"2026-07", 50L, new BigDecimal("15000.00")};

        when(paymentDashboardRepository.paymentsPerDay(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(dayRow1, dayRow2));
        when(paymentDashboardRepository.paymentsPerMonth(anyString()))
                .thenReturn(Collections.singletonList(monthRow));

        PaymentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getByDay()).hasSize(2);
        assertThat(response.getByMonth()).hasSize(1);

        PaymentDayResponse firstDay = response.getByDay().get(0);
        assertThat(firstDay.getDate()).isEqualTo(day1);
        assertThat(firstDay.getCount()).isEqualTo(3L);
        assertThat(firstDay.getAmount()).isEqualByComparingTo(new BigDecimal("750.00"));

        PaymentDayResponse secondDay = response.getByDay().get(1);
        assertThat(secondDay.getDate()).isEqualTo(day2);
        assertThat(secondDay.getCount()).isEqualTo(5L);
        assertThat(secondDay.getAmount()).isEqualByComparingTo(new BigDecimal("1250.00"));

        PaymentMonthResponse month = response.getByMonth().get(0);
        assertThat(month.getMonth()).isEqualTo("2026-07");
        assertThat(month.getCount()).isEqualTo(50L);
        assertThat(month.getAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    void calculate_whenNoPaymentsForMonth_shouldReturnEmptyByDay() {
        Object[] monthRow = new Object[]{"2026-06", 30L, new BigDecimal("9000.00")};

        when(paymentDashboardRepository.paymentsPerDay(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(paymentDashboardRepository.paymentsPerMonth(anyString()))
                .thenReturn(Collections.singletonList(monthRow));

        PaymentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getByDay()).isEmpty();
        assertThat(response.getByMonth()).hasSize(1);
    }

    @Test
    void calculate_whenNoPaymentsAtAll_shouldReturnEmptyLists() {
        when(paymentDashboardRepository.paymentsPerDay(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(paymentDashboardRepository.paymentsPerMonth(anyString()))
                .thenReturn(Collections.emptyList());

        PaymentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getByDay()).isEmpty();
        assertThat(response.getByMonth()).isEmpty();
    }

    @Test
    void calculate_withMultipleDays_shouldMaintainOrder() {
        LocalDate day1 = LocalDate.of(2026, 7, 1);
        LocalDate day2 = LocalDate.of(2026, 7, 15);
        LocalDate day3 = LocalDate.of(2026, 7, 30);
        Object[] dayRow1 = new Object[]{day1, 1L, new BigDecimal("100.00")};
        Object[] dayRow2 = new Object[]{day2, 2L, new BigDecimal("200.00")};
        Object[] dayRow3 = new Object[]{day3, 3L, new BigDecimal("300.00")};

        Object[] monthRow = new Object[]{"2026-07", 6L, new BigDecimal("600.00")};

        when(paymentDashboardRepository.paymentsPerDay(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(dayRow1, dayRow2, dayRow3));
        when(paymentDashboardRepository.paymentsPerMonth(anyString()))
                .thenReturn(Collections.singletonList(monthRow));

        PaymentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getByDay()).hasSize(3);
        assertThat(response.getByDay().get(0).getDate()).isEqualTo(day1);
        assertThat(response.getByDay().get(1).getDate()).isEqualTo(day2);
        assertThat(response.getByDay().get(2).getDate()).isEqualTo(day3);
    }

    @Test
    void calculate_withMultipleMonths_shouldReturnAllMonths() {
        Object[] dayRow = new Object[]{LocalDate.of(2026, 7, 15), 5L, new BigDecimal("1250.00")};

        Object[] month1 = new Object[]{"2026-01", 40L, new BigDecimal("10000.00")};
        Object[] month2 = new Object[]{"2026-02", 45L, new BigDecimal("11000.00")};
        Object[] month3 = new Object[]{"2026-07", 50L, new BigDecimal("15000.00")};

        when(paymentDashboardRepository.paymentsPerDay(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(dayRow));
        when(paymentDashboardRepository.paymentsPerMonth(anyString()))
                .thenReturn(Arrays.asList(month1, month2, month3));

        PaymentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getByMonth()).hasSize(3);
        assertThat(response.getByMonth().get(0).getMonth()).isEqualTo("2026-01");
        assertThat(response.getByMonth().get(1).getMonth()).isEqualTo("2026-02");
        assertThat(response.getByMonth().get(2).getMonth()).isEqualTo("2026-07");
    }
}
