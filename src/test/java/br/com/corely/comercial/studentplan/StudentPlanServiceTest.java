package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.billingschedule.BillingSchedule;
import br.com.corely.comercial.billingschedule.BillingScheduleRepository;
import br.com.corely.comercial.billingschedule.BillingFrequency;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotService;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.comercial.studentplan.dto.StudentPlanResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPlanServiceTest {

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ContractSnapshotService contractSnapshotService;

    @Mock
    private ComercialTenantContext tenantContext;

    @Mock
    private BillingScheduleRepository billingScheduleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StudentPlanService service;

    private UUID studioId;
    private Studio studio;
    private Student student;
    private UUID studentId;
    private ContractSnapshot snapshot;
    private UUID snapshotId;

    @BeforeEach
    void setUp() {
        service = new StudentPlanService(studentPlanRepository, studentRepository, studioRepository,
                contractSnapshotService, tenantContext, billingScheduleRepository, objectMapper);

        studioId = UUID.randomUUID();
        snapshotId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        studio = new Studio();
        studio.setId(studioId);

        student = new Student();
        student.setId(studentId);
        student.setFullName("John Doe");

        snapshot = new ContractSnapshot();
        snapshot.setId(snapshotId);
        snapshot.setPlanId(UUID.randomUUID());
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanDescription("Premium access");
        snapshot.setPlanPrice(BigDecimal.valueOf(199));
        snapshot.setPlanDuration(30);
        snapshot.setRules("{\"WEEKLY_CLASSES\":2}");
    }

    @Test
    void create_shouldCreateSnapshotAndPersist() {
        var planId = UUID.randomUUID();
        var request = new StudentPlanRequest();
        request.setStudentId(studentId);
        request.setPlanId(planId);
        request.setStartDate(LocalDate.now());

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)).thenReturn(false);
        when(contractSnapshotService.create(planId)).thenReturn(snapshot);
        when(studentPlanRepository.save(any(StudentPlan.class))).thenAnswer(inv -> {
            var sp = inv.getArgument(0, StudentPlan.class);
            sp.setId(UUID.randomUUID());
            sp.setCreatedAt(LocalDateTime.now());
            sp.setUpdatedAt(LocalDateTime.now());
            return sp;
        });

        StudentPlanResponse response = service.create(request);

        assertThat(response.getStudentId()).isEqualTo(studentId);
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getContractSnapshotId()).isEqualTo(snapshotId);
        assertThat(response.getSnapshotName()).isEqualTo("Gold Plan");
        assertThat(response.getPlanId()).isEqualTo(snapshot.getPlanId());
        assertThat(response.getPlanDescription()).isEqualTo("Premium access");
        assertThat(response.getPlanPrice()).isEqualByComparingTo(BigDecimal.valueOf(199));
        assertThat(response.getWeeklyClasses()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
    }

    @Test
    void create_shouldReturnNullWeeklyClasses_whenNoRules() {
        snapshot.setRules(null);
        var planId = UUID.randomUUID();
        var request = new StudentPlanRequest();
        request.setStudentId(studentId);
        request.setPlanId(planId);
        request.setStartDate(LocalDate.now());

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)).thenReturn(false);
        when(contractSnapshotService.create(planId)).thenReturn(snapshot);
        when(studentPlanRepository.save(any(StudentPlan.class))).thenAnswer(inv -> {
            var sp = inv.getArgument(0, StudentPlan.class);
            sp.setId(UUID.randomUUID());
            sp.setCreatedAt(LocalDateTime.now());
            sp.setUpdatedAt(LocalDateTime.now());
            return sp;
        });

        StudentPlanResponse response = service.create(request);

        assertThat(response.getWeeklyClasses()).isNull();
    }

    @Test
    void create_shouldReturnNullWeeklyClasses_whenNoWeeklyClassesRule() {
        snapshot.setRules("{\"VALIDITY_DAYS\":30}");
        var planId = UUID.randomUUID();
        var request = new StudentPlanRequest();
        request.setStudentId(studentId);
        request.setPlanId(planId);
        request.setStartDate(LocalDate.now());

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)).thenReturn(false);
        when(contractSnapshotService.create(planId)).thenReturn(snapshot);
        when(studentPlanRepository.save(any(StudentPlan.class))).thenAnswer(inv -> {
            var sp = inv.getArgument(0, StudentPlan.class);
            sp.setId(UUID.randomUUID());
            sp.setCreatedAt(LocalDateTime.now());
            sp.setUpdatedAt(LocalDateTime.now());
            return sp;
        });

        StudentPlanResponse response = service.create(request);

        assertThat(response.getWeeklyClasses()).isNull();
    }

    @Test
    void create_shouldThrowException_whenStudentNotFound() {
        var request = new StudentPlanRequest();
        request.setStudentId(studentId);
        request.setPlanId(UUID.randomUUID());
        request.setStartDate(LocalDate.now());

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student not found");
    }

    @Test
    void create_shouldThrowException_whenStudentAlreadyHasActivePlan() {
        var request = new StudentPlanRequest();
        request.setStudentId(studentId);
        request.setPlanId(UUID.randomUUID());
        request.setStartDate(LocalDate.now());

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student already has an active plan.");
    }

    @Test
    void cancel_shouldChangeStatus() {
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.CANCELLED)).thenReturn(false);
        when(studentPlanRepository.save(any(StudentPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.cancel(enrollment.getId());

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowException_whenNotActive() {
        var enrollment = createEnrollment(StudentPlanStatus.CANCELLED);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.cancel(enrollment.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void suspend_shouldChangeStatus() {
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.SUSPENDED)).thenReturn(false);
        when(studentPlanRepository.save(any(StudentPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.suspend(enrollment.getId());

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
    }

    @Test
    void suspend_shouldThrowException_whenNotActive() {
        var enrollment = createEnrollment(StudentPlanStatus.SUSPENDED);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.suspend(enrollment.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reactivate_shouldChangeStatus() {
        var enrollment = createEnrollment(StudentPlanStatus.SUSPENDED);
        enrollment.setCancellationDate(LocalDate.now().minusDays(5));

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(studentPlanRepository.existsByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)).thenReturn(false);
        when(studentPlanRepository.save(any(StudentPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.reactivate(enrollment.getId());

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
        assertThat(response.getCancellationDate()).isNull();
    }

    @Test
    void reactivate_shouldThrowException_whenNotSuspended() {
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> service.reactivate(enrollment.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void findById_shouldReturnStudentPlan() {
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);
        enrollment.setCreatedAt(LocalDateTime.now().minusDays(10));
        enrollment.setUpdatedAt(LocalDateTime.now());

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(billingScheduleRepository.findByStudentPlanId(enrollment.getId())).thenReturn(Optional.empty());

        var response = service.findById(enrollment.getId());

        assertThat(response.getId()).isEqualTo(enrollment.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getPlanDescription()).isEqualTo("Premium access");
        assertThat(response.getPlanPrice()).isEqualByComparingTo(BigDecimal.valueOf(199));
        assertThat(response.getWeeklyClasses()).isEqualTo(2);
        assertThat(response.getBillingCycle()).isNull();
        assertThat(response.getNextBillingDate()).isNull();
    }

    @Test
    void findById_shouldIncludeBillingSchedule_whenPresent() {
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);

        var billingSchedule = new BillingSchedule();
        billingSchedule.setId(UUID.randomUUID());
        billingSchedule.setFrequency(BillingFrequency.MONTHLY);
        billingSchedule.setBillingDay(10);
        billingSchedule.setNextBillingDate(LocalDate.now().plusMonths(1));
        billingSchedule.setActive(true);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(billingScheduleRepository.findByStudentPlanId(enrollment.getId())).thenReturn(Optional.of(billingSchedule));

        var response = service.findById(enrollment.getId());

        assertThat(response.getBillingCycle()).isEqualTo(BillingFrequency.MONTHLY);
        assertThat(response.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(response.getNextBillingFrequency()).isEqualTo(BillingFrequency.MONTHLY);
        assertThat(response.getNextBillingDay()).isEqualTo(10);
        assertThat(response.getNextBillingActive()).isTrue();
    }

    @Test
    void findById_shouldReturnNullBilling_whenInactive() {
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);

        var billingSchedule = new BillingSchedule();
        billingSchedule.setId(UUID.randomUUID());
        billingSchedule.setFrequency(BillingFrequency.MONTHLY);
        billingSchedule.setBillingDay(10);
        billingSchedule.setNextBillingDate(LocalDate.now().plusMonths(1));
        billingSchedule.setActive(false);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(billingScheduleRepository.findByStudentPlanId(enrollment.getId())).thenReturn(Optional.of(billingSchedule));

        var response = service.findById(enrollment.getId());

        assertThat(response.getBillingCycle()).isNull();
        assertThat(response.getNextBillingDate()).isNull();
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        var id = UUID.randomUUID();
        when(studentPlanRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("StudentPlan not found");
    }

    @Test
    void findAll_shouldIncludeBillingSchedules() {
        var enrollment1 = createEnrollment(StudentPlanStatus.ACTIVE);
        enrollment1.setId(UUID.randomUUID());
        var enrollment2 = createEnrollment(StudentPlanStatus.ACTIVE);
        enrollment2.setId(UUID.randomUUID());

        when(studentPlanRepository.findAll()).thenReturn(List.of(enrollment1, enrollment2));
        when(billingScheduleRepository.findByStudentPlanIdIn(List.of(enrollment1.getId(), enrollment2.getId())))
                .thenReturn(List.of());

        var results = service.findAll();

        assertThat(results).hasSize(2);
    }

    @Test
    void findById_shouldReturnNullPlanDescription_whenBlank() {
        snapshot.setPlanDescription(null);
        var enrollment = createEnrollment(StudentPlanStatus.ACTIVE);

        when(studentPlanRepository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(billingScheduleRepository.findByStudentPlanId(enrollment.getId())).thenReturn(Optional.empty());

        var response = service.findById(enrollment.getId());

        assertThat(response.getPlanDescription()).isNull();
    }

    private StudentPlan createEnrollment(StudentPlanStatus status) {
        var sp = new StudentPlan();
        sp.setId(UUID.randomUUID());
        sp.setStudent(student);
        sp.setContractSnapshot(snapshot);
        sp.setStartDate(LocalDate.now());
        sp.setStatus(status);
        sp.setCreatedAt(LocalDateTime.now());
        sp.setUpdatedAt(LocalDateTime.now());
        return sp;
    }
}
