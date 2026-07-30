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

    private final FinancialDashboardStudentPlanRepository studentPlanRepository;
    private final FinancialDashboardInvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public StudentMetricsResponse calculate(String referenceMonth) {
        long activeStudents = studentPlanRepository.countActive();
        long delinquentStudents = invoiceRepository.countDelinquentStudents();

        LocalDate monthStart = LocalDate.parse(referenceMonth + "-01");
        LocalDate monthEnd = monthStart.plusMonths(1);
        LocalDateTime dateTimeMonthStart = monthStart.atStartOfDay();
        LocalDateTime dateTimeMonthEnd = monthEnd.atStartOfDay();

        long newStudentsThisMonth = studentPlanRepository.countCreatedInMonth(dateTimeMonthStart, dateTimeMonthEnd);
        long cancellationsThisMonth = studentPlanRepository.countCancelledInMonth(monthStart, monthEnd);

        return StudentMetricsResponse.builder()
                .activeStudents(activeStudents)
                .delinquentStudents(delinquentStudents)
                .newStudentsThisMonth(newStudentsThisMonth)
                .cancellationsThisMonth(cancellationsThisMonth)
                .build();
    }
}
