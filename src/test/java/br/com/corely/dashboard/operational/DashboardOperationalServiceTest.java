package br.com.corely.dashboard.operational;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.dashboard.operational.dto.*;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.makeup.MakeupRequest;
import br.com.corely.makeup.MakeupRequestRepository;
import br.com.corely.makeup.MakeupRequestStatus;
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
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardOperationalServiceTest {

    @Autowired
    private DashboardOperationalService dashboardOperationalService;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MakeupRequestRepository makeupRequestRepository;

    private Studio studio;
    private Studio otherStudio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private Student student;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        makeupRequestRepository.deleteAll();
        attendanceRepository.deleteAll();
        classSessionRepository.deleteAll();
        enrollmentRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        studentRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        otherStudio = new Studio();
        otherStudio.setName("Other Studio");
        otherStudio = studioRepository.save(otherStudio);

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
    }

    @Test
    void dashboardVazio() {
        var response = dashboardOperationalService.getOperationalDashboard(otherStudio.getId());

        assertThat(response.getSummary().getClassesToday()).isZero();
        assertThat(response.getSummary().getClassesInProgress()).isZero();
        assertThat(response.getSummary().getStudentsPresentToday()).isZero();
        assertThat(response.getSummary().getPendingMakeupRequests()).isZero();
        assertThat(response.getUpcomingSessions()).isEmpty();
        assertThat(response.getPendingMakeupRequests()).isEmpty();
        assertThat(response.getOccupancy()).isEmpty();
        assertThat(response.getAlerts()).hasSize(1);
        assertThat(response.getAlerts().get(0).getMessage()).isEqualTo("Nenhuma aula hoje");
    }

    @Test
    void dashboardCompleto() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        Attendance attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        MakeupRequest makeup = new MakeupRequest();
        makeup.setAttendance(attendance);
        makeup.setStatus(MakeupRequestStatus.REQUESTED);
        makeup.setReason("Médico");
        makeup.setRequestedAt(LocalDateTime.now());
        makeupRequestRepository.save(makeup);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getSummary().getClassesToday()).isEqualTo(1);
        assertThat(response.getSummary().getStudentsPresentToday()).isEqualTo(1);
        assertThat(response.getSummary().getPendingMakeupRequests()).isEqualTo(1);

        assertThat(response.getUpcomingSessions()).hasSize(1);
        assertThat(response.getUpcomingSessions().get(0).getClassGroupName()).isEqualTo("Test Class Group");
        assertThat(response.getUpcomingSessions().get(0).getEnrolledStudents()).isEqualTo(1);

        assertThat(response.getPendingMakeupRequests()).hasSize(1);
        assertThat(response.getPendingMakeupRequests().get(0).getStudentName()).isEqualTo("Test Student");
        assertThat(response.getPendingMakeupRequests().get(0).getReason()).isEqualTo("Médico");

        assertThat(response.getOccupancy()).hasSize(1);
        assertThat(response.getOccupancy().get(0).getOccupancyPercentage()).isEqualTo(10);

        assertThat(response.getAlerts()).anyMatch(a -> a.getMessage().contains("Reposições pendentes"));
    }

    @Test
    void semAulas() {
        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getSummary().getClassesToday()).isZero();
        assertThat(response.getUpcomingSessions()).isEmpty();
        assertThat(response.getAlerts()).anyMatch(a -> a.getMessage().contains("Nenhuma aula hoje"));
    }

    @Test
    void semReposicoes() {
        createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getSummary().getPendingMakeupRequests()).isZero();
        assertThat(response.getPendingMakeupRequests()).isEmpty();
        assertThat(response.getAlerts()).noneMatch(a -> a.getMessage().contains("Reposições pendentes"));
    }

    @Test
    void semPresencas() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        Attendance attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.ABSENT);
        attendanceRepository.save(attendance);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getSummary().getStudentsPresentToday()).isZero();
    }

    @Test
    void ocupacao() {
        ClassGroup cg = new ClassGroup();
        cg.setStudio(studio);
        cg.setInstructor(instructor);
        cg.setName("Yoga Class");
        cg.setStartTime(LocalTime.of(14, 0));
        cg.setEndTime(LocalTime.of(15, 0));
        cg.setCapacity(20);
        cg.setMonday(true);
        cg.setActive(true);
        ClassGroup classGroup2 = classGroupRepository.save(cg);

        Student student2 = new Student();
        student2.setStudio(studio);
        student2.setFullName("Student 2");
        student2.setActive(true);
        student2 = studentRepository.save(student2);

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudio(studio);
        enrollment2.setStudent(student2);
        enrollment2.setClassGroup(classGroup2);
        enrollment2.setEnrollmentDate(LocalDate.now());
        enrollment2.setActive(true);
        enrollmentRepository.save(enrollment2);

        Student student3 = new Student();
        student3.setStudio(studio);
        student3.setFullName("Student 3");
        student3.setActive(true);
        student3 = studentRepository.save(student3);

        Enrollment enrollment3 = new Enrollment();
        enrollment3.setStudio(studio);
        enrollment3.setStudent(student3);
        enrollment3.setClassGroup(classGroup2);
        enrollment3.setEnrollmentDate(LocalDate.now());
        enrollment3.setActive(true);
        enrollmentRepository.save(enrollment3);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getOccupancy()).hasSize(2);

        var classGroupOcc = response.getOccupancy().stream()
                .filter(o -> o.getClassGroupId().equals(classGroup.getId()))
                .findFirst().orElseThrow();
        assertThat(classGroupOcc.getCapacity()).isEqualTo(10);
        assertThat(classGroupOcc.getEnrolled()).isEqualTo(1);
        assertThat(classGroupOcc.getOccupancyPercentage()).isEqualTo(10);

        var yogaOcc = response.getOccupancy().stream()
                .filter(o -> o.getClassGroupId().equals(classGroup2.getId()))
                .findFirst().orElseThrow();
        assertThat(yogaOcc.getCapacity()).isEqualTo(20);
        assertThat(yogaOcc.getEnrolled()).isEqualTo(2);
        assertThat(yogaOcc.getOccupancyPercentage()).isEqualTo(10);
    }

    @Test
    void alertas() {
        ClassGroup fullGroup = new ClassGroup();
        fullGroup.setStudio(studio);
        fullGroup.setInstructor(instructor);
        fullGroup.setName("Full Class");
        fullGroup.setStartTime(LocalTime.of(8, 0));
        fullGroup.setEndTime(LocalTime.of(9, 0));
        fullGroup.setCapacity(1);
        fullGroup.setMonday(true);
        fullGroup.setActive(true);
        fullGroup = classGroupRepository.save(fullGroup);

        enrollment.setClassGroup(fullGroup);
        enrollmentRepository.save(enrollment);

        ClassSession session = createSession(LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.IN_PROGRESS);
        session.setClassGroup(fullGroup);
        classSessionRepository.save(session);

        Attendance attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        MakeupRequest makeup = new MakeupRequest();
        makeup.setAttendance(attendance);
        makeup.setStatus(MakeupRequestStatus.REQUESTED);
        makeup.setReason("Médico");
        makeup.setRequestedAt(LocalDateTime.now());
        makeupRequestRepository.save(makeup);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getAlerts()).anyMatch(a -> a.getMessage().contains("Turma lotada"));
        assertThat(response.getAlerts()).anyMatch(a -> a.getMessage().contains("Reposições pendentes"));
        assertThat(response.getAlerts()).anyMatch(a -> a.getMessage().contains("Aulas em andamento"));
        assertThat(response.getAlerts()).noneMatch(a -> a.getMessage().contains("Nenhuma aula hoje"));
    }

    @Test
    void filtraPorStudio() {
        Instructor otherInstructor = new Instructor();
        otherInstructor.setStudio(otherStudio);
        otherInstructor.setFullName("Other Instructor");
        otherInstructor.setActive(true);
        otherInstructor = instructorRepository.save(otherInstructor);

        ClassGroup otherGroup = new ClassGroup();
        otherGroup.setStudio(otherStudio);
        otherGroup.setInstructor(otherInstructor);
        otherGroup.setName("Other Group");
        otherGroup.setStartTime(LocalTime.of(10, 0));
        otherGroup.setEndTime(LocalTime.of(11, 0));
        otherGroup.setCapacity(10);
        otherGroup.setMonday(true);
        otherGroup.setActive(true);
        otherGroup = classGroupRepository.save(otherGroup);

        Student otherStudent = new Student();
        otherStudent.setStudio(otherStudio);
        otherStudent.setFullName("Other Student");
        otherStudent.setActive(true);
        otherStudent = studentRepository.save(otherStudent);

        Enrollment otherEnrollment = new Enrollment();
        otherEnrollment.setStudio(otherStudio);
        otherEnrollment.setStudent(otherStudent);
        otherEnrollment.setClassGroup(otherGroup);
        otherEnrollment.setEnrollmentDate(LocalDate.now());
        otherEnrollment.setActive(true);
        otherEnrollment = enrollmentRepository.save(otherEnrollment);

        ClassSession otherSession = createSessionForGroup(otherGroup, otherInstructor, LocalDate.now(),
                LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        Attendance otherAttendance = new Attendance();
        otherAttendance.setClassSession(otherSession);
        otherAttendance.setEnrollment(otherEnrollment);
        otherAttendance.setStatus(AttendanceStatus.PRESENT);
        otherAttendance = attendanceRepository.save(otherAttendance);

        MakeupRequest otherMakeup = new MakeupRequest();
        otherMakeup.setAttendance(otherAttendance);
        otherMakeup.setStatus(MakeupRequestStatus.REQUESTED);
        otherMakeup.setReason("Viagem");
        otherMakeup.setRequestedAt(LocalDateTime.now());
        makeupRequestRepository.save(otherMakeup);

        var studioResponse = dashboardOperationalService.getOperationalDashboard(studio.getId());
        var otherResponse = dashboardOperationalService.getOperationalDashboard(otherStudio.getId());

        assertThat(studioResponse.getSummary().getClassesToday()).isZero();
        assertThat(studioResponse.getSummary().getPendingMakeupRequests()).isZero();
        assertThat(studioResponse.getUpcomingSessions()).isEmpty();
        assertThat(studioResponse.getOccupancy()).hasSize(1);
        assertThat(studioResponse.getAlerts()).anyMatch(a -> a.getMessage().contains("Nenhuma aula hoje"));

        assertThat(otherResponse.getSummary().getClassesToday()).isEqualTo(1);
        assertThat(otherResponse.getSummary().getPendingMakeupRequests()).isEqualTo(1);
        assertThat(otherResponse.getUpcomingSessions()).hasSize(1);
        assertThat(otherResponse.getOccupancy()).hasSize(1);
        assertThat(otherResponse.getPendingMakeupRequests()).hasSize(1);
        assertThat(otherResponse.getPendingMakeupRequests().get(0).getStudentName()).isEqualTo("Other Student");
    }

    private ClassSession createSession(LocalDate date, LocalTime start, LocalTime end, ClassSessionStatus status) {
        return createSessionForGroup(classGroup, instructor, date, start, end, status);
    }

    private ClassSession createSessionForGroup(ClassGroup group, Instructor instr,
                                                LocalDate date, LocalTime start, LocalTime end,
                                                ClassSessionStatus status) {
        ClassSession session = new ClassSession();
        session.setClassGroup(group);
        session.setInstructor(instr);
        session.setSessionDate(date);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setStatus(status);
        return classSessionRepository.save(session);
    }
}
