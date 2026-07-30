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
    private StudentDashboardRepository studentDashboardRepository;

    @Mock
    private RevenueDashboardRepository revenueDashboardRepository;

    @InjectMocks
    private StudentMetricsIndicator indicator;

    private String referenceMonth;

    @BeforeEach
    void setUp() {
        referenceMonth = "2026-07";
    }

    @Test
    void calculate_shouldReturnCorrectStudentMetrics() {
        when(studentDashboardRepository.countActive()).thenReturn(120L);
        when(revenueDashboardRepository.countDelinquentStudents()).thenReturn(8L);
        when(studentDashboardRepository.countCreatedInMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L);
        when(studentDashboardRepository.countCancelledInMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(3L);

        StudentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getActiveStudents()).isEqualTo(120L);
        assertThat(response.getDelinquentStudents()).isEqualTo(8L);
        assertThat(response.getNewStudentsThisMonth()).isEqualTo(15L);
        assertThat(response.getCancellationsThisMonth()).isEqualTo(3L);
    }

    @Test
    void calculate_whenNoData_shouldReturnZeros() {
        when(studentDashboardRepository.countActive()).thenReturn(0L);
        when(revenueDashboardRepository.countDelinquentStudents()).thenReturn(0L);
        when(studentDashboardRepository.countCreatedInMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(studentDashboardRepository.countCancelledInMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);

        StudentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getActiveStudents()).isEqualTo(0L);
        assertThat(response.getDelinquentStudents()).isEqualTo(0L);
        assertThat(response.getNewStudentsThisMonth()).isEqualTo(0L);
        assertThat(response.getCancellationsThisMonth()).isEqualTo(0L);
    }

    @Test
    void calculate_withHighDelinquency_shouldReturnCorrectRate() {
        when(studentDashboardRepository.countActive()).thenReturn(50L);
        when(revenueDashboardRepository.countDelinquentStudents()).thenReturn(25L);
        when(studentDashboardRepository.countCreatedInMonth(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(studentDashboardRepository.countCancelledInMonth(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(10L);

        StudentMetricsResponse response = indicator.calculate(referenceMonth);

        assertThat(response.getActiveStudents()).isEqualTo(50L);
        assertThat(response.getDelinquentStudents()).isEqualTo(25L);
        assertThat(response.getNewStudentsThisMonth()).isEqualTo(5L);
        assertThat(response.getCancellationsThisMonth()).isEqualTo(10L);
    }
}
