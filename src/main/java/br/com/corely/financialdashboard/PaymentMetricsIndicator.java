package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.PaymentDayResponse;
import br.com.corely.financialdashboard.dto.PaymentMetricsResponse;
import br.com.corely.financialdashboard.dto.PaymentMonthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMetricsIndicator {

    private final PaymentDashboardRepository paymentDashboardRepository;

    @Transactional(readOnly = true)
    public PaymentMetricsResponse calculate(String referenceMonth) {
        LocalDate monthStart = LocalDate.parse(referenceMonth + "-01");
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<PaymentDayResponse> byDay = calculateByDay(monthStart, monthEnd);
        List<PaymentMonthResponse> byMonth = calculateByMonth();

        return PaymentMetricsResponse.builder()
                .byDay(byDay)
                .byMonth(byMonth)
                .build();
    }

    private List<PaymentDayResponse> calculateByDay(LocalDate monthStart, LocalDate monthEnd) {
        List<Object[]> data = paymentDashboardRepository.paymentsPerDay(monthStart, monthEnd);
        List<PaymentDayResponse> result = new ArrayList<>();

        for (Object[] row : data) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            result.add(PaymentDayResponse.builder()
                    .date(date)
                    .count(count)
                    .amount(amount)
                    .build());
        }

        return result;
    }

    private List<PaymentMonthResponse> calculateByMonth() {
        LocalDate now = LocalDate.now();
        String startReferenceMonth = now.minusMonths(11).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Object[]> data = paymentDashboardRepository.paymentsPerMonth(startReferenceMonth);
        List<PaymentMonthResponse> result = new ArrayList<>();

        for (Object[] row : data) {
            String month = (String) row[0];
            Long count = (Long) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            result.add(PaymentMonthResponse.builder()
                    .month(month)
                    .count(count)
                    .amount(amount)
                    .build());
        }

        return result;
    }
}
