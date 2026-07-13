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
    void calculateNextBillingDate_shouldReturnSameMonthForWeekly() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.WEEKLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void calculateNextBillingDate_shouldReturnTwoWeeksLaterForBiweekly() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.BIWEEKLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 29));
    }

    @Test
    void calculateNextBillingDate_shouldReturnSameMonthDayForMonthly() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void calculateNextBillingDate_shouldReturnThreeMonthsLaterForQuarterly() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.QUARTERLY, 15);
        assertThat(result).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void calculateNextBillingDate_shouldReturnSixMonthsLaterForSemiannual() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.SEMIANNUAL, 15);
        assertThat(result).isEqualTo(LocalDate.of(2027, 2, 15));
    }

    @Test
    void calculateNextBillingDate_shouldReturnOneYearLaterForAnnual() {
        var start = LocalDate.of(2026, 8, 15);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.ANNUAL, 15);
        assertThat(result).isEqualTo(LocalDate.of(2027, 8, 15));
    }

    @Test
    void calculateNextBillingDate_shouldClampBillingDayToMonthLength() {
        var start = LocalDate.of(2026, 1, 31);
        var result = BillingScheduleService.calculateNextBillingDate(start, BillingFrequency.MONTHLY, 31);
        assertThat(result).isEqualTo(LocalDate.of(2026, 1, 31));
    }
}
