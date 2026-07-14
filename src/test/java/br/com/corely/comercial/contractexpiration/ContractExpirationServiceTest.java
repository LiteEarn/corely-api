package br.com.corely.comercial.contractexpiration;

import br.com.corely.comercial.billingschedule.BillingScheduleService;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractExpirationServiceTest {

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private BillingScheduleService billingScheduleService;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ContractExpirationService service;

    private Plan planAutoRenew;
    private Plan planNoAutoRenew;
    private ContractSnapshot snapshot;
    private StudentPlan expiredPlan;
    private StudentPlan expiredPlanNoAutoRenew;

    @BeforeEach
    void setUp() {
        service = new ContractExpirationService(
                studentPlanRepository, planRepository, billingScheduleService,
                transactionTemplate);

        planAutoRenew = new Plan();
        planAutoRenew.setId(UUID.randomUUID());
        planAutoRenew.setName("Auto Renew Plan");
        planAutoRenew.setAutoRenew(true);

        planNoAutoRenew = new Plan();
        planNoAutoRenew.setId(UUID.randomUUID());
        planNoAutoRenew.setName("No Auto Renew Plan");
        planNoAutoRenew.setAutoRenew(false);

        snapshot = new ContractSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setPlanId(planNoAutoRenew.getId());

        expiredPlan = new StudentPlan();
        expiredPlan.setId(UUID.randomUUID());
        expiredPlan.setStatus(StudentPlanStatus.ACTIVE);
        expiredPlan.setStartDate(LocalDate.of(2026, 6, 1));
        expiredPlan.setEndDate(LocalDate.of(2026, 7, 1));
        expiredPlan.setContractSnapshot(snapshot);
        expiredPlan.setBookingBlocked(true);

        expiredPlanNoAutoRenew = new StudentPlan();
        expiredPlanNoAutoRenew.setId(UUID.randomUUID());
        expiredPlanNoAutoRenew.setStatus(StudentPlanStatus.ACTIVE);
        expiredPlanNoAutoRenew.setStartDate(LocalDate.of(2026, 6, 1));
        expiredPlanNoAutoRenew.setEndDate(LocalDate.of(2026, 7, 1));
        expiredPlanNoAutoRenew.setContractSnapshot(snapshot);
        expiredPlanNoAutoRenew.setBookingBlocked(true);
    }

    @Test
    void process_shouldFinishContractWhenAutoRenewIsFalse() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(expiredPlanNoAutoRenew));
        when(planRepository.findById(snapshot.getPlanId()))
                .thenReturn(Optional.of(planNoAutoRenew));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getFinished()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        assertThat(expiredPlanNoAutoRenew.getStatus()).isEqualTo(StudentPlanStatus.FINISHED);
        assertThat(expiredPlanNoAutoRenew.getBookingBlocked()).isFalse();
        verify(studentPlanRepository).save(expiredPlanNoAutoRenew);
        verify(billingScheduleService).deactivateSchedule(expiredPlanNoAutoRenew);
    }

    @Test
    void process_shouldSkipWhenAutoRenewIsTrue() {
        snapshot.setPlanId(planAutoRenew.getId());

        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(expiredPlan));
        when(planRepository.findById(snapshot.getPlanId()))
                .thenReturn(Optional.of(planAutoRenew));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getFinished()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
        verify(billingScheduleService, never()).deactivateSchedule(any());
    }

    @Test
    void process_shouldHandleMultipleContracts() {
        var planNoRenew2 = new Plan();
        planNoRenew2.setId(UUID.randomUUID());
        planNoRenew2.setAutoRenew(false);

        var snapshot2 = new ContractSnapshot();
        snapshot2.setId(UUID.randomUUID());
        snapshot2.setPlanId(planNoRenew2.getId());

        var expiredPlan2 = new StudentPlan();
        expiredPlan2.setId(UUID.randomUUID());
        expiredPlan2.setStatus(StudentPlanStatus.ACTIVE);
        expiredPlan2.setContractSnapshot(snapshot2);
        expiredPlan2.setBookingBlocked(true);

        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(expiredPlanNoAutoRenew, expiredPlan2));
        when(planRepository.findById(snapshot.getPlanId()))
                .thenReturn(Optional.of(planNoAutoRenew));
        when(planRepository.findById(snapshot2.getPlanId()))
                .thenReturn(Optional.of(planNoRenew2));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getFinished()).isEqualTo(2);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, times(2)).save(any());
        verify(billingScheduleService, times(2)).deactivateSchedule(any());
    }

    @Test
    void process_shouldHandleMixedAutoRenewAndNoAutoRenew() {
        snapshot.setPlanId(planAutoRenew.getId());

        var planNoRenew2 = new Plan();
        planNoRenew2.setId(UUID.randomUUID());
        planNoRenew2.setAutoRenew(false);

        var snapshot2 = new ContractSnapshot();
        snapshot2.setId(UUID.randomUUID());
        snapshot2.setPlanId(planNoRenew2.getId());

        var expiredPlan2 = new StudentPlan();
        expiredPlan2.setId(UUID.randomUUID());
        expiredPlan2.setStatus(StudentPlanStatus.ACTIVE);
        expiredPlan2.setContractSnapshot(snapshot2);
        expiredPlan2.setBookingBlocked(true);

        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(expiredPlan, expiredPlan2));
        when(planRepository.findById(snapshot.getPlanId()))
                .thenReturn(Optional.of(planAutoRenew));
        when(planRepository.findById(snapshot2.getPlanId()))
                .thenReturn(Optional.of(planNoRenew2));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getFinished()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, times(1)).save(any());
        verify(billingScheduleService, times(1)).deactivateSchedule(any());
    }

    @Test
    void process_shouldHandleErrorWithoutInterruptingOthers() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(expiredPlanNoAutoRenew, expiredPlan));
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("DB error"));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getErrors()).isEqualTo(2);
        assertThat(result.getFinished()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
    }

    @Test
    void process_shouldNotProcessWhenNoExpiredPlans() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getFinished()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
        verify(billingScheduleService, never()).deactivateSchedule(any());
    }

    @Test
    void process_shouldSkipWhenPlanNotFound() {
        var processingDate = LocalDate.of(2026, 7, 14);

        when(studentPlanRepository.findByStatusAndEndDateBefore(
                StudentPlanStatus.ACTIVE, processingDate))
                .thenReturn(List.of(expiredPlanNoAutoRenew));
        when(planRepository.findById(snapshot.getPlanId()))
                .thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getFinished()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
        verify(studentPlanRepository, never()).save(any());
        verify(billingScheduleService, never()).deactivateSchedule(any());
    }
}
