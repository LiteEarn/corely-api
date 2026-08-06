package br.com.corely.comercial.contract;

import br.com.corely.comercial.billingschedule.BillingScheduleService;
import br.com.corely.comercial.studentplan.StudentPlanService;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.comercial.studentplan.dto.StudentPlanResponse;
import br.com.corely.finance.installment.ReceivableInstallmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContractApplicationService {

    private final StudentPlanService studentPlanService;
    private final BillingScheduleService billingScheduleService;
    private final ReceivableInstallmentService receivableInstallmentService;

    @Transactional
    public StudentPlanResponse enroll(StudentPlanRequest request) {
        var data = studentPlanService.createWithEntity(request);
        billingScheduleService.createSchedule(data.entity(), request.getStartDate().getDayOfMonth());
        receivableInstallmentService.generateForStudentPlan(data.entity());
        return data.response();
    }
}
