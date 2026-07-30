package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.StudentMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentMetricsIndicatorTest {

    @Mock
    private FinancialDashboardStudentPlanRepository studentPlanRepository;

    @Mock
    private FinancialDashboardInvoiceRepository invoiceRepository;

    @InjectMocks
    private StudentMetricsIndicator indicator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectStudentMetrics() {
        when(studentPlanRepository.countActive()).thenReturn(120L);
        when(invoiceRepository.countDelinquentStudents()).thenReturn(8L);
        when(studentPlanRepository.countCreatedInMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L);
        when(studentPlanRepository.countCancelledInMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3L);

        StudentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getActiveStudents()).isEqualTo(120L);
        assertThat(response.getDelinquentStudents()).isEqualTo(8L);
        assertThat(response.getNewStudentsThisMonth()).isEqualTo(15L);
        assertThat(response.getCancellationsThisMonth()).isEqualTo(3L);
    }

    @Test
    void calculate_whenNoData_shouldReturnZeros() {
        when(studentPlanRepository.countActive()).thenReturn(0L);
        when(invoiceRepository.countDelinquentStudents()).thenReturn(0L);
        when(studentPlanRepository.countCreatedInMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(studentPlanRepository.countCancelledInMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);

        StudentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getActiveStudents()).isEqualTo(0L);
        assertThat(response.getDelinquentStudents()).isEqualTo(0L);
        assertThat(response.getNewStudentsThisMonth()).isEqualTo(0L);
        assertThat(response.getCancellationsThisMonth()).isEqualTo(0L);
    }

    @Test
    void calculate_withHighDelinquency_shouldReturnCorrectRate() {
        when(studentPlanRepository.countActive()).thenReturn(50L);
        when(invoiceRepository.countDelinquentStudents()).thenReturn(25L);
        when(studentPlanRepository.countCreatedInMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(studentPlanRepository.countCancelledInMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);

        StudentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getActiveStudents()).isEqualTo(50L);
        assertThat(response.getDelinquentStudents()).isEqualTo(25L);
        assertThat(response.getNewStudentsThisMonth()).isEqualTo(5L);
        assertThat(response.getCancellationsThisMonth()).isEqualTo(10L);
    }

    @Test
    void calculate_tenantIsolation_shouldNotAcceptStudioId() {
        var methods = StudentMetricsIndicator.class.getDeclaredMethods();
        for (var method : methods) {
            if (method.getName().equals("calculate")) {
                assertThat(method.getParameterCount()).isEqualTo(1);
                assertThat(method.getParameterTypes()[0]).isEqualTo(String.class);
            }
        }
    }
}
