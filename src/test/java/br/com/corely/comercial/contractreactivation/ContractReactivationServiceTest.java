package br.com.corely.comercial.contractreactivation;

import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractReactivationServiceTest {

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ContractReactivationService service;

    private StudentPlan suspendedPlan;
    private StudentPlan suspendedPlanWithOverdue;
    private Invoice overdueInvoice;

    @BeforeEach
    void setUp() {
        service = new ContractReactivationService(studentPlanRepository, invoiceRepository, transactionTemplate);

        suspendedPlan = new StudentPlan();
        suspendedPlan.setId(UUID.randomUUID());
        suspendedPlan.setStatus(StudentPlanStatus.SUSPENDED);
        suspendedPlan.setBookingBlocked(true);

        suspendedPlanWithOverdue = new StudentPlan();
        suspendedPlanWithOverdue.setId(UUID.randomUUID());
        suspendedPlanWithOverdue.setStatus(StudentPlanStatus.SUSPENDED);

        overdueInvoice = new Invoice();
        overdueInvoice.setId(UUID.randomUUID());
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
    }

    @Test
    void process_shouldReactivateWhenNoOverdueInvoices() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.SUSPENDED))
                .thenReturn(List.of(suspendedPlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                suspendedPlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getReactivated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(suspendedPlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
        assertThat(suspendedPlan.getBookingBlocked()).isFalse();
        verify(studentPlanRepository).save(suspendedPlan);
    }

    @Test
    void process_shouldSkipWhenOverdueInvoicesExist() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.SUSPENDED))
                .thenReturn(List.of(suspendedPlan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                suspendedPlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getReactivated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(suspendedPlan.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldHandleMultiplePlans() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.SUSPENDED))
                .thenReturn(List.of(suspendedPlan, suspendedPlanWithOverdue));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                suspendedPlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                suspendedPlanWithOverdue.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getReactivated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(suspendedPlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
        assertThat(suspendedPlanWithOverdue.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
        verify(studentPlanRepository).save(suspendedPlan);
        verify(studentPlanRepository, never()).save(suspendedPlanWithOverdue);
    }

    @Test
    void process_shouldHandleErrorWithoutInterruptingOthers() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.SUSPENDED))
                .thenReturn(List.of(suspendedPlan, suspendedPlanWithOverdue));
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("DB error"));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getErrors()).isEqualTo(2);
        assertThat(result.getReactivated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldNotProcessWhenNoSuspendedPlans() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatus(StudentPlanStatus.SUSPENDED))
                .thenReturn(List.of());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getReactivated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }
}