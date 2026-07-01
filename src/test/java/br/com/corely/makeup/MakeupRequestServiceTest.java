package br.com.corely.makeup;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.makeup.dto.MakeupApproveRequest;
import br.com.corely.makeup.dto.MakeupRejectRequest;
import br.com.corely.makeup.dto.MakeupRequestRequest;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MakeupRequestServiceTest {

    @Autowired
    private MakeupRequestService makeupRequestService;

    @Autowired
    private MakeupRequestRepository makeupRequestRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudioRepository studioRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private Student student;
    private Enrollment enrollment;
    private ClassSession completedSession;
    private ClassSession futureTargetSession;
    private Attendance absentAttendance;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        enrollmentRepository.deleteAll();
        classSessionRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        studentRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName("Test Class Group");
        classGroup.setStartTime(LocalTime.of(10, 0));
        classGroup.setEndTime(LocalTime.of(11, 0));
        classGroup.setCapacity(10);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        classGroup = classGroupRepository.save(classGroup);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setEmail("student@test.com");
        student.setActive(true);
        student = studentRepository.save(student);

        enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        completedSession = new ClassSession();
        completedSession.setClassGroup(classGroup);
        completedSession.setInstructor(instructor);
        completedSession.setSessionDate(LocalDate.now());
        completedSession.setStartTime(LocalTime.of(10, 0));
        completedSession.setEndTime(LocalTime.of(11, 0));
        completedSession.setStatus(ClassSessionStatus.COMPLETED);
        completedSession = classSessionRepository.save(completedSession);

        futureTargetSession = new ClassSession();
        futureTargetSession.setClassGroup(classGroup);
        futureTargetSession.setInstructor(instructor);
        futureTargetSession.setSessionDate(LocalDate.now().plusDays(7));
        futureTargetSession.setStartTime(LocalTime.of(10, 0));
        futureTargetSession.setEndTime(LocalTime.of(11, 0));
        futureTargetSession.setStatus(ClassSessionStatus.SCHEDULED);
        futureTargetSession = classSessionRepository.save(futureTargetSession);

        absentAttendance = new Attendance();
        absentAttendance.setClassSession(completedSession);
        absentAttendance.setEnrollment(enrollment);
        absentAttendance.setStatus(AttendanceStatus.ABSENT);
        absentAttendance = attendanceRepository.save(absentAttendance);
    }

    @Test
    void request_validMakeupRequest() {
        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");

        var response = makeupRequestService.request(absentAttendance.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.attendanceId()).isEqualTo(absentAttendance.getId());
        assertThat(response.status()).isEqualTo(MakeupRequestStatus.REQUESTED);
        assertThat(response.reason()).isEqualTo("Sick day");
        assertThat(response.requestedAt()).isNotNull();
    }

    @Test
    void request_attendanceNotFound() {
        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");

        assertThatThrownBy(() -> makeupRequestService.request(java.util.UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Attendance not found");
    }

    @Test
    void request_attendancePresent() {
        Attendance attendance = new Attendance();
        attendance.setClassSession(completedSession);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.PRESENT);
        Attendance savedAttendance = attendanceRepository.save(attendance);

        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");

        assertThatThrownBy(() -> makeupRequestService.request(savedAttendance.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only allowed for absent attendances");
    }

    @Test
    void request_attendanceJustified() {
        Attendance attendance = new Attendance();
        attendance.setClassSession(completedSession);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.JUSTIFIED);
        Attendance savedAttendance = attendanceRepository.save(attendance);

        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");

        assertThatThrownBy(() -> makeupRequestService.request(savedAttendance.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only allowed for absent attendances");
    }

    @Test
    void request_duplicateMakeupRequest() {
        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");
        makeupRequestService.request(absentAttendance.getId(), request);

        assertThatThrownBy(() -> makeupRequestService.request(absentAttendance.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A makeup request already exists for this attendance");
    }

    @Test
    void request_reasonOmitted() {
        MakeupRequestRequest request = new MakeupRequestRequest();

        var response = makeupRequestService.request(absentAttendance.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.attendanceId()).isEqualTo(absentAttendance.getId());
        assertThat(response.reason()).isNull();
    }

    @Test
    void findByAttendanceId_returnsMakeupRequest() {
        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");
        makeupRequestService.request(absentAttendance.getId(), request);

        var response = makeupRequestService.findByAttendanceId(absentAttendance.getId());

        assertThat(response).isNotNull();
        assertThat(response.attendanceId()).isEqualTo(absentAttendance.getId());
        assertThat(response.status()).isEqualTo(MakeupRequestStatus.REQUESTED);
    }

    @Test
    void findByAttendanceId_attendanceNotFound() {
        assertThatThrownBy(() -> makeupRequestService.findByAttendanceId(java.util.UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Attendance not found");
    }

    @Test
    void findByAttendanceId_makeupRequestNotFound() {
        assertThatThrownBy(() -> makeupRequestService.findByAttendanceId(absentAttendance.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Makeup request not found for this attendance");
    }

    @Test
    void findAll_returnsAllMakeupRequests() {
        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");
        makeupRequestService.request(absentAttendance.getId(), request);

        var result = makeupRequestService.findAll(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).attendanceId()).isEqualTo(absentAttendance.getId());
    }

    @Test
    void findAll_emptyList() {
        var result = makeupRequestService.findAll(null, null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_filterByStatus() {
        MakeupRequestRequest request = new MakeupRequestRequest("Sick day");
        makeupRequestService.request(absentAttendance.getId(), request);

        var result = makeupRequestService.findAll(MakeupRequestStatus.REQUESTED, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_filterByStatus_noMatch() {
        var result = makeupRequestService.findAll(MakeupRequestStatus.APPROVED, null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void request_withoutReason() {
        MakeupRequestRequest request = new MakeupRequestRequest(null);

        var response = makeupRequestService.request(absentAttendance.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.reason()).isNull();
    }

    @Test
    void approve_validMakeupRequest() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        MakeupApproveRequest approveRequest = new MakeupApproveRequest(futureTargetSession.getId());

        var response = makeupRequestService.approve(requested.id(), approveRequest);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(MakeupRequestStatus.APPROVED);
        assertThat(response.targetSessionId()).isEqualTo(futureTargetSession.getId());
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    void reject_validMakeupRequest() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        MakeupRejectRequest rejectRequest = new MakeupRejectRequest("Not a valid reason");

        var response = makeupRequestService.reject(requested.id(), rejectRequest);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(MakeupRequestStatus.REJECTED);
        assertThat(response.rejectedAt()).isNotNull();
        assertThat(response.rejectionReason()).isEqualTo("Not a valid reason");
    }

    @Test
    void reject_validMakeupRequest_withoutReason() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        MakeupRejectRequest rejectRequest = new MakeupRejectRequest();

        var response = makeupRequestService.reject(requested.id(), rejectRequest);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(MakeupRequestStatus.REJECTED);
        assertThat(response.rejectedAt()).isNotNull();
        assertThat(response.rejectionReason()).isNull();
    }

    @Test
    void approve_sessionNotFound() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        MakeupApproveRequest approveRequest = new MakeupApproveRequest(java.util.UUID.randomUUID());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Class session not found");
    }

    @Test
    void approve_makeupRequestNotFound() {
        MakeupApproveRequest approveRequest = new MakeupApproveRequest(futureTargetSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(java.util.UUID.randomUUID(), approveRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Makeup request not found");
    }

    @Test
    void reject_makeupRequestNotFound() {
        MakeupRejectRequest rejectRequest = new MakeupRejectRequest("Reason");

        assertThatThrownBy(() -> makeupRequestService.reject(java.util.UUID.randomUUID(), rejectRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Makeup request not found");
    }

    @Test
    void approve_sessionCancelled() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));

        ClassSession cancelledSession = new ClassSession();
        cancelledSession.setClassGroup(classGroup);
        cancelledSession.setInstructor(instructor);
        cancelledSession.setSessionDate(LocalDate.now().plusDays(7));
        cancelledSession.setStartTime(LocalTime.of(10, 0));
        cancelledSession.setEndTime(LocalTime.of(11, 0));
        cancelledSession.setStatus(ClassSessionStatus.CANCELLED);
        cancelledSession = classSessionRepository.save(cancelledSession);

        MakeupApproveRequest approveRequest = new MakeupApproveRequest(cancelledSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula escolhida está cancelada.");
    }

    @Test
    void approve_sessionCompleted() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));

        ClassSession completedTarget = new ClassSession();
        completedTarget.setClassGroup(classGroup);
        completedTarget.setInstructor(instructor);
        completedTarget.setSessionDate(LocalDate.now().plusDays(7));
        completedTarget.setStartTime(LocalTime.of(10, 0));
        completedTarget.setEndTime(LocalTime.of(11, 0));
        completedTarget.setStatus(ClassSessionStatus.COMPLETED);
        completedTarget = classSessionRepository.save(completedTarget);

        MakeupApproveRequest approveRequest = new MakeupApproveRequest(completedTarget.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula escolhida já foi concluída.");
    }

    @Test
    void approve_sessionInProgress() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));

        ClassSession inProgressSession = new ClassSession();
        inProgressSession.setClassGroup(classGroup);
        inProgressSession.setInstructor(instructor);
        inProgressSession.setSessionDate(LocalDate.now().plusDays(7));
        inProgressSession.setStartTime(LocalTime.of(10, 0));
        inProgressSession.setEndTime(LocalTime.of(11, 0));
        inProgressSession.setStatus(ClassSessionStatus.IN_PROGRESS);
        inProgressSession = classSessionRepository.save(inProgressSession);

        MakeupApproveRequest approveRequest = new MakeupApproveRequest(inProgressSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula escolhida já iniciou.");
    }

    @Test
    void approve_sessionInPast() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));

        ClassSession pastSession = new ClassSession();
        pastSession.setClassGroup(classGroup);
        pastSession.setInstructor(instructor);
        pastSession.setSessionDate(LocalDate.now().minusDays(1));
        pastSession.setStartTime(LocalTime.of(10, 0));
        pastSession.setEndTime(LocalTime.of(11, 0));
        pastSession.setStatus(ClassSessionStatus.SCHEDULED);
        pastSession = classSessionRepository.save(pastSession);

        MakeupApproveRequest approveRequest = new MakeupApproveRequest(pastSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A reposição deve ser marcada para uma aula futura.");
    }

    @Test
    void approve_classFull() {
        classGroup.setCapacity(1);
        classGroup = classGroupRepository.save(classGroup);

        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        MakeupApproveRequest approveRequest = new MakeupApproveRequest(futureTargetSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A turma está lotada.");
    }

    @Test
    void approve_studentAlreadyEnrolled() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));

        Attendance existingAttendance = new Attendance();
        existingAttendance.setClassSession(futureTargetSession);
        existingAttendance.setEnrollment(enrollment);
        existingAttendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(existingAttendance);

        MakeupApproveRequest approveRequest = new MakeupApproveRequest(futureTargetSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("O aluno já participa desta aula.");
    }

    @Test
    void approve_alreadyApproved() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        MakeupApproveRequest approveRequest = new MakeupApproveRequest(futureTargetSession.getId());
        makeupRequestService.approve(requested.id(), approveRequest);

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A reposição já foi processada.");
    }

    @Test
    void approve_alreadyRejected() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        makeupRequestService.reject(requested.id(), new MakeupRejectRequest("Reason"));

        MakeupApproveRequest approveRequest = new MakeupApproveRequest(futureTargetSession.getId());

        assertThatThrownBy(() -> makeupRequestService.approve(requested.id(), approveRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A reposição já foi processada.");
    }

    @Test
    void reject_alreadyRejected() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        makeupRequestService.reject(requested.id(), new MakeupRejectRequest("Reason"));

        assertThatThrownBy(() -> makeupRequestService.reject(requested.id(), new MakeupRejectRequest("Another reason")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A reposição já foi processada.");
    }

    @Test
    void reject_alreadyApproved() {
        var requested = makeupRequestService.request(absentAttendance.getId(), new MakeupRequestRequest("Sick day"));
        makeupRequestService.approve(requested.id(), new MakeupApproveRequest(futureTargetSession.getId()));

        assertThatThrownBy(() -> makeupRequestService.reject(requested.id(), new MakeupRejectRequest("Reason")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A reposição já foi processada.");
    }
}
