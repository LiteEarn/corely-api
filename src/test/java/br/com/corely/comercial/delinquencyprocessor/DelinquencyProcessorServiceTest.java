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
import br.com.corely.comercial.studentplan.SuspensionReason;
import br.com.corely.studio.Studio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DelinquencyProcessorServiceTest {

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DelinquencyPolicyRepository delinquencyPolicyRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private DelinquencyProcessorService service;

    private Studio studio;
    private StudentPlan activePlan;
    private StudentPlan activePlanWithoutOverdue;
    private Invoice overdueInvoice;
    private DelinquencyPolicy suspendPolicy;
    private DelinquencyPolicy blockPolicy;
    private DelinquencyPolicy nonePolicy;

    @BeforeEach
    void setUp() {
        service = new DelinquencyProcessorService(studentPlanRepository, invoiceRepository,
                delinquencyPolicyRepository, transactionTemplate);

        studio = new Studio();
        studio.setId(UUID.randomUUID());

        activePlan = new StudentPlan();
        activePlan.setId(UUID.randomUUID());
        activePlan.setStatus(StudentPlanStatus.ACTIVE);
        activePlan.setStudio(studio);

        activePlanWithoutOverdue = new StudentPlan();
        activePlanWithoutOverdue.setId(UUID.randomUUID());
        activePlanWithoutOverdue.setStatus(StudentPlanStatus.ACTIVE);
        activePlanWithoutOverdue.setStudio(studio);

        overdueInvoice = new Invoice();
        overdueInvoice.setId(UUID.randomUUID());
        overdueInvoice.setDueDate(LocalDate.of(2026, 1, 15));
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);

        suspendPolicy = new DelinquencyPolicy();
        suspendPolicy.setGracePeriodDays(5);
        suspendPolicy.setAction(DelinquencyAction.SUSPEND_CONTRACT);
        suspendPolicy.setActive(true);

        blockPolicy = new DelinquencyPolicy();
        blockPolicy.setGracePeriodDays(5);
        blockPolicy.setAction(DelinquencyAction.BLOCK_NEW_BOOKINGS);
        blockPolicy.setActive(true);

        nonePolicy = new DelinquencyPolicy();
        nonePolicy.setGracePeriodDays(5);
        nonePolicy.setAction(DelinquencyAction.NONE);
        nonePolicy.setActive(true);
    }

    @Test
    void process_shouldSuspendContractWhenGracePeriodExceeded() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(delinquencyPolicyRepository.findByStudioId(studio.getId()))
                .thenReturn(Optional.of(suspendPolicy));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(1);
        assertThat(result.getBlocked()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(activePlan.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
        assertThat(activePlan.getSuspensionReason()).isEqualTo(SuspensionReason.DELINQUENCY);
        verify(studentPlanRepository).save(activePlan);
    }

    @Test
    void process_shouldBlockNewBookingsWhenActionIsBlock() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(delinquencyPolicyRepository.findByStudioId(studio.getId()))
                .thenReturn(Optional.of(blockPolicy));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getBlocked()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(activePlan.getBookingBlocked()).isTrue();
        assertThat(activePlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
        verify(studentPlanRepository).save(activePlan);
    }

    @Test
    void process_shouldSkipWhenActionIsNone() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(delinquencyPolicyRepository.findByStudioId(studio.getId()))
                .thenReturn(Optional.of(nonePolicy));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getBlocked()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldSkipWhenWithinGracePeriod() {
        var processingDate = LocalDate.of(2026, 1, 18);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(delinquencyPolicyRepository.findByStudioId(studio.getId()))
                .thenReturn(Optional.of(suspendPolicy));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(activePlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldSkipWhenNoOverdueInvoices() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getBlocked()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldSkipWhenNoPolicyExists() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(delinquencyPolicyRepository.findByStudioId(studio.getId()))
                .thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldSkipWhenPolicyIsInactive() {
        var inactivePolicy = new DelinquencyPolicy();
        inactivePolicy.setActive(false);

        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(delinquencyPolicyRepository.findByStudioId(studio.getId()))
                .thenReturn(Optional.of(inactivePolicy));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldHandleErrorWithoutInterruptingOthers() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan, activePlanWithoutOverdue));
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("DB error"));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getErrors()).isEqualTo(2);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldNotProcessWhenNoActivePlans() {
        var processingDate = LocalDate.of(2026, 1, 25);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.ACTIVE))
                .thenReturn(List.of());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getBlocked()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }
}