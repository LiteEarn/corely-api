package br.com.corely.comercial.studentplan;

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
                contractSnapshotService, tenantContext);

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
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanPrice(BigDecimal.valueOf(199));
        snapshot.setPlanDuration(30);
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
            return sp;
        });

        StudentPlanResponse response = service.create(request);

        assertThat(response.getStudentId()).isEqualTo(studentId);
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getContractSnapshotId()).isEqualTo(snapshotId);
        assertThat(response.getSnapshotName()).isEqualTo("Gold Plan");
        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
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
    void findById_shouldThrowException_whenNotFound() {
        var id = UUID.randomUUID();
        when(studentPlanRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("StudentPlan not found");
    }

    private StudentPlan createEnrollment(StudentPlanStatus status) {
        var sp = new StudentPlan();
        sp.setId(UUID.randomUUID());
        sp.setStudent(student);
        sp.setContractSnapshot(snapshot);
        sp.setStartDate(LocalDate.now());
        sp.setStatus(status);
        return sp;
    }
}
