package br.com.corely.comercial.contractexpiration;

import br.com.corely.comercial.billingschedule.BillingScheduleService;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Service
public class ContractExpirationService {

    private static final Logger log = LoggerFactory.getLogger(ContractExpirationService.class);

    private final StudentPlanRepository studentPlanRepository;
    private final PlanRepository planRepository;
    private final BillingScheduleService billingScheduleService;
    private final TransactionTemplate transactionTemplate;

    public ContractExpirationService(StudentPlanRepository studentPlanRepository,
                                     PlanRepository planRepository,
                                     BillingScheduleService billingScheduleService,
                                     TransactionTemplate transactionTemplate) {
        this.studentPlanRepository = studentPlanRepository;
        this.planRepository = planRepository;
        this.billingScheduleService = billingScheduleService;
        this.transactionTemplate = transactionTemplate;
    }

    public ContractExpirationResult process(LocalDate processingDate) {
        var result = new ContractExpirationResult();
        var expiredPlans = studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate);

        for (var studentPlan : expiredPlans) {
            result.incrementProcessed();
            try {
                transactionTemplate.execute(status -> {
                    processExpiration(studentPlan, result);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error finishing StudentPlan {}: {}", studentPlan.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    private void processExpiration(StudentPlan studentPlan, ContractExpirationResult result) {
        if (studentPlan.getStatus() != StudentPlanStatus.ACTIVE) {
            log.info("StudentPlan {} skipped: status is {}", studentPlan.getId(), studentPlan.getStatus());
            result.incrementSkipped();
            return;
        }

        var snapshot = studentPlan.getContractSnapshot();
        var plan = planRepository.findById(snapshot.getPlanId())
                .orElse(null);
        if (plan == null) {
            log.warn("StudentPlan {} skipped: plan not found", studentPlan.getId());
            result.incrementSkipped();
            return;
        }

        if (plan.getAutoRenew()) {
            log.info("StudentPlan {} skipped: plan {} allows auto-renewal",
                    studentPlan.getId(), plan.getId());
            result.incrementSkipped();
            return;
        }

        studentPlan.setStatus(StudentPlanStatus.FINISHED);
        studentPlan.setBookingBlocked(false);
        studentPlanRepository.save(studentPlan);

        billingScheduleService.deactivateSchedule(studentPlan);

        log.info("StudentPlan {} finished: contract expired and auto-renew is disabled",
                studentPlan.getId());
        result.incrementFinished();
    }
}
