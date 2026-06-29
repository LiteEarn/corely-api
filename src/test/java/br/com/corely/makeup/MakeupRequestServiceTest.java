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
}
