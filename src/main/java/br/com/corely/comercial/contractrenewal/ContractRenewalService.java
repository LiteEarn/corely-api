package br.com.corely.comercial.contractrenewal;

import br.com.corely.comercial.billingschedule.BillingScheduleService;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotService;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Service
public class ContractRenewalService {

    private static final Logger log = LoggerFactory.getLogger(ContractRenewalService.class);

    private final StudentPlanRepository studentPlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final PlanRepository planRepository;
    private final ContractSnapshotService contractSnapshotService;
    private final BillingScheduleService billingScheduleService;
    private final TransactionTemplate transactionTemplate;

    public ContractRenewalService(StudentPlanRepository studentPlanRepository,
                                  InvoiceRepository invoiceRepository,
                                  PlanRepository planRepository,
                                  ContractSnapshotService contractSnapshotService,
                                  BillingScheduleService billingScheduleService,
                                  TransactionTemplate transactionTemplate) {
        this.studentPlanRepository = studentPlanRepository;
        this.invoiceRepository = invoiceRepository;
        this.planRepository = planRepository;
        this.contractSnapshotService = contractSnapshotService;
        this.billingScheduleService = billingScheduleService;
        this.transactionTemplate = transactionTemplate;
    }

    public ContractRenewalResult process(LocalDate processingDate) {
        var result = new ContractRenewalResult();
        var expiringPlans = studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate);

        for (var studentPlan : expiringPlans) {
            result.incrementProcessed();
            try {
                transactionTemplate.execute(status -> {
                    processRenewal(studentPlan, result);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error renewing StudentPlan {}: {}", studentPlan.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    private void processRenewal(StudentPlan studentPlan, ContractRenewalResult result) {
        var snapshot = studentPlan.getContractSnapshot();
        var plan = planRepository.findById(snapshot.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (!plan.getAutoRenew()) {
            log.info("StudentPlan {} skipped: plan {} does not allow auto-renewal",
                    studentPlan.getId(), plan.getId());
            result.incrementSkipped();
            return;
        }

        var overdueInvoices = invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                studentPlan.getId(), InvoiceStatus.OVERDUE);
        if (!overdueInvoices.isEmpty()) {
            log.info("StudentPlan {} skipped: has overdue invoices", studentPlan.getId());
            result.incrementSkipped();
            return;
        }

        var newSnapshot = contractSnapshotService.create(plan.getId());

        var newEndDate = studentPlan.getEndDate().plusDays(newSnapshot.getPlanDuration());
        studentPlan.setEndDate(newEndDate);
        studentPlan.setContractSnapshot(newSnapshot);
        studentPlanRepository.save(studentPlan);

        billingScheduleService.renewSchedule(studentPlan);

        log.info("StudentPlan {} renewed: new endDate {}, new snapshot {}",
                studentPlan.getId(), newEndDate, newSnapshot.getId());
        result.incrementRenewed();
    }
}
