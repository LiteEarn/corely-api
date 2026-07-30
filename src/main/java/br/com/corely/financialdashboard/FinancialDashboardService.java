package br.com.corely.financialdashboard;

import br.com.corely.financialdashboard.dto.FinancialDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialDashboardService {

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getDashboard(String referenceMonth) {
        return new FinancialDashboardResponse();
    }
}
