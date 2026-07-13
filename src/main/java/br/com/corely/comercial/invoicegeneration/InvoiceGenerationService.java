package br.com.corely.comercial.invoicegeneration;

import br.com.corely.comercial.billingschedule.BillingFrequency;
import br.com.corely.comercial.billingschedule.BillingSchedule;
import br.com.corely.comercial.billingschedule.BillingScheduleRepository;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class InvoiceGenerationService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceGenerationService.class);

    private final BillingScheduleRepository billingScheduleRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionTemplate transactionTemplate;

    public InvoiceGenerationService(BillingScheduleRepository billingScheduleRepository,
                                    InvoiceRepository invoiceRepository,
                                    TransactionTemplate transactionTemplate) {
        this.billingScheduleRepository = billingScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public InvoiceGenerationResult process(LocalDate processingDate) {
        var result = new InvoiceGenerationResult();
        var schedules = billingScheduleRepository.findByActiveTrueAndNextBillingDateLessThanEqual(processingDate);

        for (var schedule : schedules) {
            result.incrementProcessed();
            try {
                transactionTemplate.execute(status -> {
                    processSchedule(schedule.getId(), processingDate, result);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error processing billing schedule {}: {}", schedule.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    public void processSchedule(UUID scheduleId, LocalDate processingDate, InvoiceGenerationResult result) {
        var schedule = billingScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("BillingSchedule not found: " + scheduleId));
        var studentPlan = schedule.getStudentPlan();

        if (studentPlan.getStatus() != StudentPlanStatus.ACTIVE) {
            log.warn("BillingSchedule {} skipped: StudentPlan {} is not ACTIVE", schedule.getId(), studentPlan.getId());
            result.incrementSkipped();
            return;
        }

        var referenceMonth = schedule.getNextBillingDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        if (invoiceRepository.findByStudentPlanIdAndReferenceMonth(studentPlan.getId(), referenceMonth).isPresent()) {
            log.warn("BillingSchedule {} skipped: invoice already exists for {}", schedule.getId(), referenceMonth);
            result.incrementSkipped();
            return;
        }

        var snapshot = studentPlan.getContractSnapshot();
        var invoice = new Invoice();
        invoice.setStudio(studentPlan.getStudio());
        invoice.setStudentPlan(studentPlan);
        invoice.setDueDate(schedule.getNextBillingDate());
        invoice.setReferenceMonth(referenceMonth);
        invoice.setAmount(snapshot.getPlanPrice());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setIssueDate(processingDate);
        invoiceRepository.save(invoice);

        var nextDate = advanceNextBillingDate(schedule.getNextBillingDate(), schedule.getFrequency(), schedule.getBillingDay());
        schedule.setNextBillingDate(nextDate);
        billingScheduleRepository.save(schedule);

        result.incrementGenerated();
    }

    static LocalDate advanceNextBillingDate(LocalDate currentDate, BillingFrequency frequency, int billingDay) {
        return switch (frequency) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case BIWEEKLY -> currentDate.plusWeeks(2);
            case MONTHLY -> withBillingDay(currentDate.plusMonths(1), billingDay);
            case QUARTERLY -> withBillingDay(currentDate.plusMonths(3), billingDay);
            case SEMIANNUAL -> withBillingDay(currentDate.plusMonths(6), billingDay);
            case ANNUAL -> withBillingDay(currentDate.plusYears(1), billingDay);
        };
    }

    private static LocalDate withBillingDay(LocalDate target, int billingDay) {
        int clamped = Math.min(billingDay, target.lengthOfMonth());
        return target.withDayOfMonth(clamped);
    }
}
