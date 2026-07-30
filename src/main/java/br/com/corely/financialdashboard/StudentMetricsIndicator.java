package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.StudentMetricsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentMetricsIndicator {

    private final StudentDashboardRepository studentDashboardRepository;
    private final RevenueDashboardRepository revenueDashboardRepository;

    @Transactional(readOnly = true)
    public StudentMetricsResponse calculate(String referenceMonth) {
        long activeStudents = studentDashboardRepository.countActive();
        long delinquentStudents = revenueDashboardRepository.countDelinquentStudents();

        LocalDate monthStart = LocalDate.parse(referenceMonth + "-01");
        LocalDate monthEnd = monthStart.plusMonths(1);
        LocalDateTime dateTimeMonthStart = monthStart.atStartOfDay();
        LocalDateTime dateTimeMonthEnd = monthEnd.atStartOfDay();

        long newStudentsThisMonth = studentDashboardRepository.countCreatedInMonth(dateTimeMonthStart, dateTimeMonthEnd);
        long cancellationsThisMonth = studentDashboardRepository.countCancelledInMonth(monthStart, monthEnd);

        return StudentMetricsResponse.builder()
                .activeStudents(activeStudents)
                .delinquentStudents(delinquentStudents)
                .newStudentsThisMonth(newStudentsThisMonth)
                .cancellationsThisMonth(cancellationsThisMonth)
                .build();
    }
}
