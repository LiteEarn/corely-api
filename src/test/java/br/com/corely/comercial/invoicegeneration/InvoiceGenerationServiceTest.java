package br.com.corely.comercial.invoicegeneration;

import br.com.corely.comercial.billingschedule.BillingFrequency;
import br.com.corely.comercial.billingschedule.BillingSchedule;
import br.com.corely.comercial.billingschedule.BillingScheduleRepository;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceGenerationServiceTest {

    @Mock
    private BillingScheduleRepository billingScheduleRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    @Captor
    private ArgumentCaptor<BillingSchedule> scheduleCaptor;

    private InvoiceGenerationService service;

    private Studio studio;
    private Student student;
    private ContractSnapshot snapshot;
    private StudentPlan studentPlan;
    private BillingSchedule schedule;
    private UUID scheduleId;
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        service = new InvoiceGenerationService(billingScheduleRepository, invoiceRepository);

        studio = new Studio();
        studio.setId(UUID.randomUUID());

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Jane Doe");

        snapshot = new ContractSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanPrice(BigDecimal.valueOf(199));

        studentPlanId = UUID.randomUUID();
        studentPlan = new StudentPlan();
        studentPlan.setId(studentPlanId);
        studentPlan.setStudent(student);
        studentPlan.setContractSnapshot(snapshot);
        studentPlan.setStatus(StudentPlanStatus.ACTIVE);
        studentPlan.setStudio(studio);

        scheduleId = UUID.randomUUID();
        schedule = new BillingSchedule();
        schedule.setId(scheduleId);
        schedule.setStudentPlan(studentPlan);
        schedule.setFrequency(BillingFrequency.MONTHLY);
        schedule.setBillingDay(15);
        schedule.setNextBillingDate(LocalDate.of(2026, 1, 15));
        schedule.setActive(true);
    }

    @Test
    void process_shouldGenerateInvoiceAndUpdateNextBillingDate() {
        var processingDate = LocalDate.of(2026, 1, 20);

        when(billingScheduleRepository.findByActiveTrueAndNextBillingDateLessThanEqual(processingDate))
                .thenReturn(List.of(schedule));
        when(invoiceRepository.findByStudentPlanIdAndReferenceMonth(studentPlanId, "2026-01"))
                .thenReturn(Optional.empty());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getGenerated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);

        verify(invoiceRepository).save(invoiceCaptor.capture());
        var invoice = invoiceCaptor.getValue();
        assertThat(invoice.getStudentPlan()).isEqualTo(studentPlan);
        assertThat(invoice.getReferenceMonth()).isEqualTo("2026-01");
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(invoice.getAmount()).isEqualByComparingTo(new BigDecimal("199"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getIssueDate()).isEqualTo(processingDate);

        verify(billingScheduleRepository).save(scheduleCaptor.capture());
        var updatedSchedule = scheduleCaptor.getValue();
        assertThat(updatedSchedule.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void process_shouldSkipWhenStudentPlanNotActive() {
        studentPlan.setStatus(StudentPlanStatus.SUSPENDED);

        var processingDate = LocalDate.of(2026, 1, 20);

        when(billingScheduleRepository.findByActiveTrueAndNextBillingDateLessThanEqual(processingDate))
                .thenReturn(List.of(schedule));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getGenerated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);

        verify(invoiceRepository, never()).save(any());
        verify(billingScheduleRepository, never()).save(any());
    }

    @Test
    void process_shouldSkipWhenInvoiceAlreadyExists() {
        var processingDate = LocalDate.of(2026, 1, 20);

        when(billingScheduleRepository.findByActiveTrueAndNextBillingDateLessThanEqual(processingDate))
                .thenReturn(List.of(schedule));
        when(invoiceRepository.findByStudentPlanIdAndReferenceMonth(studentPlanId, "2026-01"))
                .thenReturn(Optional.of(new Invoice()));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getGenerated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);

        verify(invoiceRepository, never()).save(any());
        verify(billingScheduleRepository, never()).save(any());
    }

    @Test
    void process_shouldHandleErrorWithoutInterruptingOthers() {
        var scheduleWithError = new BillingSchedule();
        scheduleWithError.setId(UUID.randomUUID());
        scheduleWithError.setFrequency(BillingFrequency.MONTHLY);

        var processingDate = LocalDate.of(2026, 1, 20);

        when(billingScheduleRepository.findByActiveTrueAndNextBillingDateLessThanEqual(processingDate))
                .thenReturn(List.of(scheduleWithError, schedule));
        when(invoiceRepository.findByStudentPlanIdAndReferenceMonth(studentPlanId, "2026-01"))
                .thenReturn(Optional.empty());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getGenerated()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(1);
    }

    @Test
    void process_shouldNotGenerateWhenNoSchedules() {
        var processingDate = LocalDate.of(2026, 1, 20);

        when(billingScheduleRepository.findByActiveTrueAndNextBillingDateLessThanEqual(processingDate))
                .thenReturn(List.of());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getGenerated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void advanceNextBillingDate_weekly_shouldAddOneWeek() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 15), BillingFrequency.WEEKLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 1, 22));
    }

    @Test
    void advanceNextBillingDate_biweekly_shouldAddTwoWeeks() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 15), BillingFrequency.BIWEEKLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 1, 29));
    }

    @Test
    void advanceNextBillingDate_monthly_shouldAddOneMonth() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 15), BillingFrequency.MONTHLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void advanceNextBillingDate_quarterly_shouldAddThreeMonths() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 15), BillingFrequency.QUARTERLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 15));
    }

    @Test
    void advanceNextBillingDate_semiannual_shouldAddSixMonths() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 15), BillingFrequency.SEMIANNUAL, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void advanceNextBillingDate_annual_shouldAddOneYear() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 15), BillingFrequency.ANNUAL, 15);
        assertThat(result).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    void advanceNextBillingDate_shouldClampToMonthEnd() {
        var result = InvoiceGenerationService.advanceNextBillingDate(
                LocalDate.of(2026, 1, 31), BillingFrequency.MONTHLY, 31);
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
