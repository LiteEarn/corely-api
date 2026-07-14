package br.com.corely.comercial.contractreactivation;

import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.studentplan.SuspensionReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Service
public class ContractReactivationService {

    private static final Logger log = LoggerFactory.getLogger(ContractReactivationService.class);

    private final StudentPlanRepository studentPlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionTemplate transactionTemplate;

    public ContractReactivationService(StudentPlanRepository studentPlanRepository,
                                       InvoiceRepository invoiceRepository,
                                       TransactionTemplate transactionTemplate) {
        this.studentPlanRepository = studentPlanRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public ContractReactivationResult process(LocalDate processingDate) {
        var result = new ContractReactivationResult();
        var suspendedPlans = studentPlanRepository.findByStatus(StudentPlanStatus.SUSPENDED);

        for (var studentPlan : suspendedPlans) {
            result.incrementProcessed();
            try {
                transactionTemplate.execute(status -> {
                    processStudentPlan(studentPlan, result);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error reactivating StudentPlan {}: {}", studentPlan.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    private void processStudentPlan(StudentPlan studentPlan, ContractReactivationResult result) {
        if (studentPlan.getSuspensionReason() != SuspensionReason.DELINQUENCY) {
            result.incrementSkipped();
            return;
        }

        var overdueInvoices = invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                studentPlan.getId(), InvoiceStatus.OVERDUE);

        if (!overdueInvoices.isEmpty()) {
            result.incrementSkipped();
            return;
        }

        studentPlan.setStatus(StudentPlanStatus.ACTIVE);
        studentPlan.setBookingBlocked(false);
        studentPlan.setSuspensionReason(null);
        studentPlanRepository.save(studentPlan);

        log.info("StudentPlan {} reactivated from SUSPENDED to ACTIVE", studentPlan.getId());
        result.incrementReactivated();
    }
}