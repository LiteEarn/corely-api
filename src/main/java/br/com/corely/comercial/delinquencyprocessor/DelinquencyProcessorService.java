package br.com.corely.comercial.delinquencyprocessor;

import br.com.corely.comercial.delinquencypolicy.DelinquencyAction;
import br.com.corely.comercial.delinquencypolicy.DelinquencyPolicy;
import br.com.corely.comercial.delinquencypolicy.DelinquencyPolicyRepository;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DelinquencyProcessorService {

    private static final Logger log = LoggerFactory.getLogger(DelinquencyProcessorService.class);

    private final StudentPlanRepository studentPlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final DelinquencyPolicyRepository delinquencyPolicyRepository;
    private final TransactionTemplate transactionTemplate;

    public DelinquencyProcessorService(StudentPlanRepository studentPlanRepository,
                                       InvoiceRepository invoiceRepository,
                                       DelinquencyPolicyRepository delinquencyPolicyRepository,
                                       TransactionTemplate transactionTemplate) {
        this.studentPlanRepository = studentPlanRepository;
        this.invoiceRepository = invoiceRepository;
        this.delinquencyPolicyRepository = delinquencyPolicyRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public DelinquencyProcessorResult process(LocalDate processingDate) {
        var result = new DelinquencyProcessorResult();
        var activeStudentPlans = studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE);

        for (var studentPlan : activeStudentPlans) {
            result.incrementProcessed();
            try {
                transactionTemplate.execute(status -> {
                    processStudentPlan(studentPlan, processingDate, result);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error processing delinquency for StudentPlan {}: {}", studentPlan.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    private void processStudentPlan(StudentPlan studentPlan, LocalDate processingDate, DelinquencyProcessorResult result) {
        var overdueInvoices = invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                studentPlan.getId(), InvoiceStatus.OVERDUE);

        if (overdueInvoices.isEmpty()) {
            result.incrementSkipped();
            return;
        }

        var oldestOverdue = overdueInvoices.get(0);
        var daysOverdue = processingDate.toEpochDay() - oldestOverdue.getDueDate().toEpochDay();

        var studioId = studentPlan.getStudio().getId();
        var policyOptional = delinquencyPolicyRepository.findByStudioId(studioId);

        if (policyOptional.isEmpty() || !policyOptional.get().getActive()) {
            result.incrementSkipped();
            return;
        }

        var policy = policyOptional.get();

        if (daysOverdue <= policy.getGracePeriodDays()) {
            result.incrementSkipped();
            return;
        }

        switch (policy.getAction()) {
            case NONE -> result.incrementSkipped();
            case BLOCK_NEW_BOOKINGS -> {
                studentPlan.setBookingBlocked(true);
                studentPlanRepository.save(studentPlan);
                log.info("BLOCK_NEW_BOOKINGS action applied for StudentPlan {}", studentPlan.getId());
                result.incrementBlocked();
            }
            case SUSPEND_CONTRACT -> {
                if (studentPlan.getStatus() == StudentPlanStatus.ACTIVE) {
                    studentPlan.setStatus(StudentPlanStatus.SUSPENDED);
                    studentPlanRepository.save(studentPlan);
                    log.info("StudentPlan {} suspended due to delinquency", studentPlan.getId());
                    result.incrementSuspended();
                } else {
                    result.incrementSkipped();
                }
            }
        }
    }
}