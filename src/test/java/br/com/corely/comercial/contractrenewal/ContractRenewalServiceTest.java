package br.com.corely.comercial.contractrenewal;

import br.com.corely.comercial.billingschedule.BillingScheduleService;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotService;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractRenewalServiceTest {

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ContractSnapshotService contractSnapshotService;

    @Mock
    private BillingScheduleService billingScheduleService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ContractRenewalService service;

    private Plan plan;
    private ContractSnapshot oldSnapshot;
    private ContractSnapshot newSnapshot;
    private StudentPlan activePlan;
    private StudentPlan activePlanWithOverdue;
    private StudentPlan activePlanNoAutoRenew;
    private Invoice overdueInvoice;

    @BeforeEach
    void setUp() {
        service = new ContractRenewalService(
                studentPlanRepository, invoiceRepository, planRepository,
                contractSnapshotService, billingScheduleService,
                transactionTemplate);

        plan = new Plan();
        plan.setId(UUID.randomUUID());
        plan.setName("Test Plan");
        plan.setDuration(30);
        plan.setAutoRenew(true);

        oldSnapshot = new ContractSnapshot();
        oldSnapshot.setId(UUID.randomUUID());
        oldSnapshot.setPlanId(plan.getId());
        oldSnapshot.setPlanDuration(30);
        oldSnapshot.setPlanPrice(BigDecimal.valueOf(100));

        newSnapshot = new ContractSnapshot();
        newSnapshot.setId(UUID.randomUUID());
        newSnapshot.setPlanId(plan.getId());
        newSnapshot.setPlanDuration(30);
        newSnapshot.setPlanPrice(BigDecimal.valueOf(100));

        activePlan = new StudentPlan();
        activePlan.setId(UUID.randomUUID());
        activePlan.setStatus(StudentPlanStatus.ACTIVE);
        activePlan.setStartDate(LocalDate.of(2026, 6, 1));
        activePlan.setEndDate(LocalDate.of(2026, 7, 1));
        activePlan.setContractSnapshot(oldSnapshot);

        activePlanWithOverdue = new StudentPlan();
        activePlanWithOverdue.setId(UUID.randomUUID());
        activePlanWithOverdue.setStatus(StudentPlanStatus.ACTIVE);
        activePlanWithOverdue.setStartDate(LocalDate.of(2026, 6, 1));
        activePlanWithOverdue.setEndDate(LocalDate.of(2026, 7, 1));
        activePlanWithOverdue.setContractSnapshot(oldSnapshot);

        activePlanNoAutoRenew = new StudentPlan();
        activePlanNoAutoRenew.setId(UUID.randomUUID());
        activePlanNoAutoRenew.setStatus(StudentPlanStatus.ACTIVE);
        activePlanNoAutoRenew.setStartDate(LocalDate.of(2026, 6, 1));
        activePlanNoAutoRenew.setEndDate(LocalDate.of(2026, 7, 1));
        activePlanNoAutoRenew.setContractSnapshot(oldSnapshot);

        overdueInvoice = new Invoice();
        overdueInvoice.setId(UUID.randomUUID());
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
    }

    @Test
    void process_shouldRenewWhenEligible() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(activePlan));
        when(planRepository.findById(oldSnapshot.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(contractSnapshotService.create(plan.getId()))
                .thenReturn(newSnapshot);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getRenewed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(activePlan.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(activePlan.getContractSnapshot()).isEqualTo(newSnapshot);
        verify(studentPlanRepository).save(activePlan);
        verify(billingScheduleService).renewSchedule(activePlan);
    }

    @Test
    void process_shouldSkipWhenPlanDoesNotAllowAutoRenew() {
        var planNoAutoRenew = new Plan();
        planNoAutoRenew.setId(UUID.randomUUID());
        planNoAutoRenew.setAutoRenew(false);
        oldSnapshot.setPlanId(planNoAutoRenew.getId());

        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(activePlanNoAutoRenew));
        when(planRepository.findById(oldSnapshot.getPlanId()))
                .thenReturn(Optional.of(planNoAutoRenew));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getRenewed()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
        verify(contractSnapshotService, never()).create(any());
    }

    @Test
    void process_shouldSkipWhenOverdueInvoicesExist() {
        oldSnapshot.setPlanId(plan.getId());

        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(activePlanWithOverdue));
        when(planRepository.findById(oldSnapshot.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlanWithOverdue.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getRenewed()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
        verify(contractSnapshotService, never()).create(any());
    }

    @Test
    void process_shouldHandleMultiplePlans() {
        oldSnapshot.setPlanId(plan.getId());

        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(activePlan, activePlanWithOverdue));
        when(planRepository.findById(oldSnapshot.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlanWithOverdue.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of(overdueInvoice));
        when(contractSnapshotService.create(plan.getId()))
                .thenReturn(newSnapshot);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getRenewed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository).save(activePlan);
        verify(studentPlanRepository, never()).save(activePlanWithOverdue);
    }

    @Test
    void process_shouldHandleErrorWithoutInterruptingOthers() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(activePlan, activePlanWithOverdue));
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("DB error"));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getErrors()).isEqualTo(2);
        assertThat(result.getRenewed()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldNotProcessWhenNoExpiringPlans() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getRenewed()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
        verify(contractSnapshotService, never()).create(any());
    }

    @Test
    void process_shouldDelegateBillingScheduleRenewal() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateLessThanEqual(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(activePlan));
        when(planRepository.findById(oldSnapshot.getPlanId()))
                .thenReturn(Optional.of(plan));
        when(invoiceRepository.findByStudentPlanIdAndStatusOrderByDueDateAsc(
                activePlan.getId(), InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(contractSnapshotService.create(plan.getId()))
                .thenReturn(newSnapshot);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        service.process(processingDate);

        verify(billingScheduleService).renewSchedule(activePlan);
        verify(billingScheduleService, never()).createSchedule(any(), anyInt());
    }
}
