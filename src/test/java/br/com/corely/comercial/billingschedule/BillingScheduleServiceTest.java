package br.com.corely.comercial.billingschedule;

import br.com.corely.comercial.billingschedule.dto.BillingFrequencyDto;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleRequest;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingScheduleServiceTest {

    @Mock
    private BillingScheduleRepository billingScheduleRepository;

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ComercialTenantContext tenantContext;

    private BillingScheduleService service;

    private UUID studioId;
    private Studio studio;
    private Student student;
    private ContractSnapshot snapshot;
    private StudentPlan studentPlan;
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        service = new BillingScheduleService(billingScheduleRepository, studentPlanRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Jane Doe");

        snapshot = new ContractSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanPrice(BigDecimal.valueOf(199));
        snapshot.setPlanDuration(30);

        studentPlanId = UUID.randomUUID();
        studentPlan = new StudentPlan();
        studentPlan.setId(studentPlanId);
        studentPlan.setStudent(student);
        studentPlan.setContractSnapshot(snapshot);
        studentPlan.setStatus(StudentPlanStatus.ACTIVE);
        studentPlan.setStartDate(LocalDate.of(2026, 8, 15));
    }

    @Test
    void create_shouldCreateSchedule() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(billingScheduleRepository.save(any(BillingSchedule.class))).thenAnswer(inv -> {
            var bs = inv.getArgument(0, BillingSchedule.class);
            bs.setId(UUID.randomUUID());
            return bs;
        });

        var response = service.create(studentPlan, 15);

        assertThat(response).isNotNull();
        assertThat(response.getFrequency()).isEqualTo(BillingFrequencyDto.MONTHLY);
        assertThat(response.getBillingDay()).isEqualTo(15);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getStudentName()).isEqualTo("Jane Doe");
        assertThat(response.getPlanName()).isEqualTo("Gold Plan");
    }

    @Test
    void update_shouldChangeFrequencyAndBillingDay() {
        var schedule = new BillingSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setStudentPlan(studentPlan);
        schedule.setFrequency(BillingFrequency.MONTHLY);
        schedule.setBillingDay(15);
        schedule.setNextBillingDate(LocalDate.of(2026, 8, 15));
        schedule.setActive(true);

        when(billingScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(billingScheduleRepository.save(any(BillingSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new BillingScheduleRequest();
        request.setFrequency(BillingFrequencyDto.QUARTERLY);
        request.setBillingDay(10);

        var response = service.update(schedule.getId(), request);

        assertThat(response.getFrequency()).isEqualTo(BillingFrequencyDto.QUARTERLY);
        assertThat(response.getBillingDay()).isEqualTo(10);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        when(billingScheduleRepository.findById(any())).thenReturn(Optional.empty());

        var request = new BillingScheduleRequest();
        request.setFrequency(BillingFrequencyDto.MONTHLY);
        request.setBillingDay(15);

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("BillingSchedule not found");
    }

    @Test
    void findById_shouldReturnSchedule() {
        var schedule = new BillingSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setStudentPlan(studentPlan);
        schedule.setFrequency(BillingFrequency.MONTHLY);
        schedule.setBillingDay(15);
        schedule.setNextBillingDate(LocalDate.of(2026, 8, 15));
        schedule.setActive(true);

        when(billingScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        var response = service.findById(schedule.getId());

        assertThat(response.getId()).isEqualTo(schedule.getId());
        assertThat(response.getStudentName()).isEqualTo("Jane Doe");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(billingScheduleRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("BillingSchedule not found");
    }

    @Test
    void billingDayBeforeStartDate_shouldAdvanceToNextMonth() {
        var start = LocalDate.of(2026, 1, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 10));
    }

    @Test
    void billingDayAfterStartDate_shouldUseSameMonth() {
        var start = LocalDate.of(2026, 1, 5);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void billingDayEqualToStartDate_shouldUseSameMonth() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void billingDay31InMonthWith30Days_shouldClampTo30() {
        var start = LocalDate.of(2026, 4, 20);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 31);
        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    void billingDay31InFebruaryNonLeap_shouldClampTo28() {
        var start = LocalDate.of(2026, 2, 1);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 31);
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void billingDay31InFebruaryLeap_shouldClampTo29() {
        var start = LocalDate.of(2024, 2, 1);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 31);
        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void weekly_billingDayBeforeStart_shouldAdvanceToNextMonth() {
        var start = LocalDate.of(2026, 1, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.WEEKLY, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 10));
    }

    @Test
    void weekly_billingDayAfterStart_shouldUseSameMonth() {
        var start = LocalDate.of(2026, 1, 5);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.WEEKLY, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    void biweekly_shouldAddTwoWeeksThenAdjust() {
        var start = LocalDate.of(2026, 1, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.BIWEEKLY, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 2, 10));
    }

    @Test
    void quarterly_shouldAddThreeMonthsThenAdjust() {
        var start = LocalDate.of(2026, 1, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.QUARTERLY, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void semiannual_shouldAddSixMonthsThenAdjust() {
        var start = LocalDate.of(2026, 1, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.SEMIANNUAL, 10);
        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void annual_shouldAddOneYearThenAdjust() {
        var start = LocalDate.of(2026, 1, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.ANNUAL, 10);
        assertThat(result).isEqualTo(LocalDate.of(2027, 1, 10));
    }

    @Test
    void annual_billingDayAfterStart_shouldUseSameMonth() {
        var start = LocalDate.of(2026, 1, 5);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.ANNUAL, 10);
        assertThat(result).isEqualTo(LocalDate.of(2027, 1, 10));
    }
}
