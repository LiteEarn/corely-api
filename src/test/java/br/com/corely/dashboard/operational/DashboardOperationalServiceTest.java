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
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

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

    private DashboardKpiResponse kpis(DashboardOperationalResponse r) {
        return r.getSummary().getKpis();
    }

    @Test
    void dashboardVazio() {
        var response = dashboardOperationalService.getOperationalDashboard(otherStudio.getId());

        assertThat(kpis(response).getClassesToday()).isZero();
        assertThat(kpis(response).getClassesInProgress()).isZero();
        assertThat(kpis(response).getActiveStudents()).isZero();
        assertThat(kpis(response).getStudentsPresentToday()).isZero();
        assertThat(kpis(response).getPendingMakeups()).isZero();
        assertThat(response.getUpcomingSessions()).isEmpty();
        assertThat(response.getPendingMakeupRequests()).isEmpty();
        assertThat(response.getClassOccupancy()).isEmpty();
        assertThat(response.getSummary().getAverageOccupancy()).isZero();
        assertThat(response.getSummary().getTodayAttendanceRate()).isZero();
        assertThat(response.getAlerts()).hasSize(1);
        assertThat(response.getAlerts().get(0).getType()).isEqualTo(AlertType.NO_CLASSES);
    }

    @Test
    void dashboardCompleto() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        Attendance attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        ClassGroup makeupGroup = new ClassGroup();
        makeupGroup.setStudio(studio);
        makeupGroup.setInstructor(instructor);
        makeupGroup.setName("Makeup Group");
        makeupGroup.setStartTime(LocalTime.of(10, 0));
        makeupGroup.setEndTime(LocalTime.of(11, 0));
        makeupGroup.setCapacity(100);
        makeupGroup.setMonday(true);
        makeupGroup.setActive(true);
        makeupGroup = classGroupRepository.save(makeupGroup);

        for (int i = 0; i < 11; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(makeupGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);

            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.ABSENT);
            a = attendanceRepository.save(a);

            MakeupRequest mr = new MakeupRequest();
            mr.setAttendance(a);
            mr.setStatus(MakeupRequestStatus.REQUESTED);
            mr.setReason("Test " + i);
            mr.setRequestedAt(LocalDateTime.now());
            makeupRequestRepository.save(mr);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(kpis(response).getClassesToday()).isEqualTo(1);
        assertThat(kpis(response).getStudentsPresentToday()).isEqualTo(1);
        assertThat(kpis(response).getPendingMakeups()).isEqualTo(11);

        assertThat(response.getUpcomingSessions()).hasSize(1);
        assertThat(response.getUpcomingSessions().get(0).getClassName()).isEqualTo("Test Class Group");

        assertThat(response.getPendingMakeupRequests()).hasSize(5);
        assertThat(response.getPendingMakeupRequests().get(0).getStudentName()).isEqualTo("Student 0");
        assertThat(response.getPendingMakeupRequests().get(0).getClassName()).isEqualTo("Makeup Group");
        assertThat(response.getPendingMakeupRequests().get(0).getClassGroupId()).isEqualTo(makeupGroup.getId());

        assertThat(response.getClassOccupancy()).hasSize(2);

        assertThat(response.getAlerts()).anyMatch(a -> a.getType() == AlertType.PENDING_MAKEUP);
    }

    @Test
    void semAulas() {
        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(kpis(response).getClassesToday()).isZero();
        assertThat(response.getUpcomingSessions()).isEmpty();
        assertThat(response.getAlerts()).anyMatch(a -> a.getType() == AlertType.NO_CLASSES);
    }

    @Test
    void semReposicoes() {
        createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(kpis(response).getPendingMakeups()).isZero();
        assertThat(response.getPendingMakeupRequests()).isEmpty();
        assertThat(response.getAlerts()).noneMatch(a -> a.getType() == AlertType.PENDING_MAKEUP);
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

        assertThat(kpis(response).getStudentsPresentToday()).isZero();
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

        assertThat(response.getClassOccupancy()).hasSize(2);

        var classGroupOcc = response.getClassOccupancy().stream()
                .filter(o -> o.getClassGroupId().equals(classGroup.getId()))
                .findFirst().orElseThrow();
        assertThat(classGroupOcc.getCapacity()).isEqualTo(10);
        assertThat(classGroupOcc.getEnrolled()).isEqualTo(1);
        assertThat(classGroupOcc.getOccupancyPercent()).isEqualTo(10);

        var yogaOcc = response.getClassOccupancy().stream()
                .filter(o -> o.getClassGroupId().equals(classGroup2.getId()))
                .findFirst().orElseThrow();
        assertThat(yogaOcc.getCapacity()).isEqualTo(20);
        assertThat(yogaOcc.getEnrolled()).isEqualTo(2);
        assertThat(yogaOcc.getOccupancyPercent()).isEqualTo(10);
    }

    @Test
    void alertaOcupacao() {
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

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getAlerts()).anyMatch(a -> a.getType() == AlertType.FULL_CLASS);
    }

    @Test
    void alertaReposicoes() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        for (int i = 0; i < 11; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(classGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);

            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.ABSENT);
            a = attendanceRepository.save(a);

            MakeupRequest mr = new MakeupRequest();
            mr.setAttendance(a);
            mr.setStatus(MakeupRequestStatus.REQUESTED);
            mr.setReason("Test " + i);
            mr.setRequestedAt(LocalDateTime.now());
            makeupRequestRepository.save(mr);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getAlerts()).anyMatch(a -> a.getType() == AlertType.PENDING_MAKEUP);
    }

    @Test
    void alertaNenhumaAula() {
        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getAlerts()).anyMatch(a -> a.getType() == AlertType.NO_CLASSES);
    }

    @Test
    void ocupacaoCorreta() {
        ClassGroup cg = new ClassGroup();
        cg.setStudio(studio);
        cg.setInstructor(instructor);
        cg.setName("Half Full");
        cg.setStartTime(LocalTime.of(14, 0));
        cg.setEndTime(LocalTime.of(15, 0));
        cg.setCapacity(10);
        cg.setMonday(true);
        cg.setActive(true);
        ClassGroup halfGroup = classGroupRepository.save(cg);

        for (int i = 0; i < 5; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(halfGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            enrollmentRepository.save(e);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        var halfOcc = response.getClassOccupancy().stream()
                .filter(o -> o.getClassGroupId().equals(halfGroup.getId()))
                .findFirst().orElseThrow();
        assertThat(halfOcc.getOccupancyPercent()).isEqualTo(50);
    }

    @Test
    void resumoCorreto() {
        createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);
        createSession(LocalDate.now(), LocalTime.of(11, 0), LocalTime.of(12, 0), ClassSessionStatus.IN_PROGRESS);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(kpis(response).getClassesToday()).isEqualTo(2);
        assertThat(kpis(response).getClassesInProgress()).isEqualTo(1);
    }

    @Test
    void listaOrdenada() {
        ClassGroup cg1 = new ClassGroup();
        cg1.setStudio(studio);
        cg1.setInstructor(instructor);
        cg1.setName("Group A");
        cg1.setStartTime(LocalTime.of(10, 0));
        cg1.setEndTime(LocalTime.of(11, 0));
        cg1.setCapacity(10);
        cg1.setMonday(true);
        cg1.setActive(true);
        cg1 = classGroupRepository.save(cg1);

        ClassGroup cg2 = new ClassGroup();
        cg2.setStudio(studio);
        cg2.setInstructor(instructor);
        cg2.setName("Group B");
        cg2.setStartTime(LocalTime.of(14, 0));
        cg2.setEndTime(LocalTime.of(15, 0));
        cg2.setCapacity(10);
        cg2.setMonday(true);
        cg2.setActive(true);
        cg2 = classGroupRepository.save(cg2);

        for (int i = 0; i < 8; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(cg1);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            enrollmentRepository.save(e);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getClassOccupancy()).hasSize(3);
        assertThat(response.getClassOccupancy().get(0).getOccupancyPercent())
                .isGreaterThanOrEqualTo(response.getClassOccupancy().get(1).getOccupancyPercent());
        assertThat(response.getClassOccupancy().get(1).getOccupancyPercent())
                .isGreaterThanOrEqualTo(response.getClassOccupancy().get(2).getOccupancyPercent());
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

        assertThat(kpis(studioResponse).getClassesToday()).isZero();
        assertThat(kpis(studioResponse).getPendingMakeups()).isZero();
        assertThat(studioResponse.getUpcomingSessions()).isEmpty();
        assertThat(studioResponse.getClassOccupancy()).hasSize(1);
        assertThat(studioResponse.getAlerts()).anyMatch(a -> a.getType() == AlertType.NO_CLASSES);

        assertThat(kpis(otherResponse).getClassesToday()).isEqualTo(1);
        assertThat(kpis(otherResponse).getPendingMakeups()).isEqualTo(1);
        assertThat(otherResponse.getUpcomingSessions()).hasSize(1);
        assertThat(otherResponse.getClassOccupancy()).hasSize(1);
        assertThat(otherResponse.getPendingMakeupRequests()).hasSize(1);
        assertThat(otherResponse.getPendingMakeupRequests().get(0).getStudentName()).isEqualTo("Other Student");
    }

    @Test
    void limite5Sessoes() {
        for (int i = 0; i < 7; i++) {
            ClassGroup g = new ClassGroup();
            g.setStudio(studio);
            g.setInstructor(instructor);
            g.setName("Group " + i);
            g.setStartTime(LocalTime.of(10 + i, 0));
            g.setEndTime(LocalTime.of(11 + i, 0));
            g.setCapacity(10);
            g.setMonday(true);
            g.setActive(true);
            g = classGroupRepository.save(g);
            createSessionForGroup(g, instructor, LocalDate.now(),
                    LocalTime.of(10 + i, 0), LocalTime.of(11 + i, 0), ClassSessionStatus.SCHEDULED);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getUpcomingSessions()).hasSize(5);
    }

    @Test
    void limite5Reposicoes() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        for (int i = 0; i < 7; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(classGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);

            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.ABSENT);
            a = attendanceRepository.save(a);

            MakeupRequest mr = new MakeupRequest();
            mr.setAttendance(a);
            mr.setStatus(MakeupRequestStatus.REQUESTED);
            mr.setReason("Test " + i);
            mr.setRequestedAt(LocalDateTime.now().plusMinutes(i));
            makeupRequestRepository.save(mr);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getPendingMakeupRequests()).hasSize(5);
    }

    @Test
    void limite5Ocupacoes() {
        for (int i = 0; i < 7; i++) {
            ClassGroup g = new ClassGroup();
            g.setStudio(studio);
            g.setInstructor(instructor);
            g.setName("Group " + i);
            g.setStartTime(LocalTime.of(10, 0));
            g.setEndTime(LocalTime.of(11, 0));
            g.setCapacity(10);
            g.setMonday(true);
            g.setActive(true);
            classGroupRepository.save(g);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getClassOccupancy()).hasSize(5);
    }

    @Test
    void ordenacaoSessoes() {
        createSession(LocalDate.now(), LocalTime.of(14, 0), LocalTime.of(15, 0), ClassSessionStatus.SCHEDULED);
        createSession(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), ClassSessionStatus.IN_PROGRESS);
        createSession(LocalDate.now(), LocalTime.of(11, 0), LocalTime.of(12, 0), ClassSessionStatus.SCHEDULED);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        List<UpcomingSessionResponse> sessions = response.getUpcomingSessions();
        assertThat(sessions).hasSize(3);
        assertThat(sessions.get(0).getStatus()).isEqualTo(ClassSessionStatus.IN_PROGRESS);
        assertThat(sessions.get(1).getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
        assertThat(sessions.get(2).getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
        assertThat(sessions.get(1).getStartTime()).isBefore(sessions.get(2).getStartTime());
    }

    @Test
    void ordenacaoReposicoes() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        List<Student> students = IntStream.range(0, 5).mapToObj(i -> {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(classGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);

            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.ABSENT);
            a = attendanceRepository.save(a);

            MakeupRequest mr = new MakeupRequest();
            mr.setAttendance(a);
            mr.setStatus(MakeupRequestStatus.REQUESTED);
            mr.setReason("Test " + i);
            mr.setRequestedAt(LocalDateTime.now().plusMinutes(i * 10));
            return makeupRequestRepository.save(mr);
        }).toList();

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        List<PendingMakeupResponse> makeups = response.getPendingMakeupRequests();
        assertThat(makeups).hasSize(5);
        assertThat(makeups.get(0).getId()).isEqualTo(students.get(0).getId());
    }

    @Test
    void averageOccupancy() {
        ClassGroup cg1 = new ClassGroup();
        cg1.setStudio(studio);
        cg1.setInstructor(instructor);
        cg1.setName("Group 50");
        cg1.setStartTime(LocalTime.of(8, 0));
        cg1.setEndTime(LocalTime.of(9, 0));
        cg1.setCapacity(10);
        cg1.setMonday(true);
        cg1.setActive(true);
        cg1 = classGroupRepository.save(cg1);

        ClassGroup cg2 = new ClassGroup();
        cg2.setStudio(studio);
        cg2.setInstructor(instructor);
        cg2.setName("Group 75");
        cg2.setStartTime(LocalTime.of(9, 0));
        cg2.setEndTime(LocalTime.of(10, 0));
        cg2.setCapacity(4);
        cg2.setMonday(true);
        cg2.setActive(true);
        cg2 = classGroupRepository.save(cg2);

        ClassGroup cg3 = new ClassGroup();
        cg3.setStudio(studio);
        cg3.setInstructor(instructor);
        cg3.setName("Group 100");
        cg3.setStartTime(LocalTime.of(10, 0));
        cg3.setEndTime(LocalTime.of(11, 0));
        cg3.setCapacity(1);
        cg3.setMonday(true);
        cg3.setActive(true);
        cg3 = classGroupRepository.save(cg3);

        for (int i = 0; i < 5; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student G1-" + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(cg1);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            enrollmentRepository.save(e);
        }
        for (int i = 0; i < 3; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student G2-" + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(cg2);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            enrollmentRepository.save(e);
        }
        {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student G3");
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(cg3);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            enrollmentRepository.save(e);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getSummary().getAverageOccupancy()).isEqualTo(75);
    }

    @Test
    void todayAttendanceRate() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        for (int i = 0; i < 3; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student A-" + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(classGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);
            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.PRESENT);
            attendanceRepository.save(a);
        }
        for (int i = 0; i < 2; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student B-" + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(classGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);
            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.ABSENT);
            attendanceRepository.save(a);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getSummary().getTodayAttendanceRate()).isEqualTo(60);
    }

    @Test
    void alertasPrioridade() {
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

        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        for (int i = 0; i < 11; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment e = new Enrollment();
            e.setStudio(studio);
            e.setStudent(s);
            e.setClassGroup(fullGroup);
            e.setEnrollmentDate(LocalDate.now());
            e.setActive(true);
            e = enrollmentRepository.save(e);
            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(e);
            a.setStatus(AttendanceStatus.ABSENT);
            a = attendanceRepository.save(a);
            MakeupRequest mr = new MakeupRequest();
            mr.setAttendance(a);
            mr.setStatus(MakeupRequestStatus.REQUESTED);
            mr.setReason("Test " + i);
            mr.setRequestedAt(LocalDateTime.now());
            makeupRequestRepository.save(mr);
        }

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        List<DashboardAlertResponse> alerts = response.getAlerts();
        assertThat(alerts).isNotEmpty();
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.FULL_CLASS);
        assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.ERROR);
    }

    @Test
    void alertasSemDuplicatas() {
        ClassGroup fullGroup1 = new ClassGroup();
        fullGroup1.setStudio(studio);
        fullGroup1.setInstructor(instructor);
        fullGroup1.setName("Full Class 1");
        fullGroup1.setStartTime(LocalTime.of(8, 0));
        fullGroup1.setEndTime(LocalTime.of(9, 0));
        fullGroup1.setCapacity(1);
        fullGroup1.setMonday(true);
        fullGroup1.setActive(true);
        fullGroup1 = classGroupRepository.save(fullGroup1);

        ClassGroup fullGroup2 = new ClassGroup();
        fullGroup2.setStudio(studio);
        fullGroup2.setInstructor(instructor);
        fullGroup2.setName("Full Class 2");
        fullGroup2.setStartTime(LocalTime.of(9, 0));
        fullGroup2.setEndTime(LocalTime.of(10, 0));
        fullGroup2.setCapacity(1);
        fullGroup2.setMonday(true);
        fullGroup2.setActive(true);
        fullGroup2 = classGroupRepository.save(fullGroup2);

        Student s = new Student();
        s.setStudio(studio);
        s.setFullName("Extra Student");
        s.setActive(true);
        s = studentRepository.save(s);
        Enrollment e = new Enrollment();
        e.setStudio(studio);
        e.setStudent(s);
        e.setClassGroup(fullGroup2);
        e.setEnrollmentDate(LocalDate.now());
        e.setActive(true);
        enrollmentRepository.save(e);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        long fullClassAlerts = response.getAlerts().stream()
                .filter(a -> a.getType() == AlertType.FULL_CLASS)
                .count();
        assertThat(fullClassAlerts).isLessThanOrEqualTo(1);
    }

    @Test
    void alertasMax5() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        for (int i = 0; i < 11; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setActive(true);
            s = studentRepository.save(s);
            Enrollment enr = new Enrollment();
            enr.setStudio(studio);
            enr.setStudent(s);
            enr.setClassGroup(classGroup);
            enr.setEnrollmentDate(LocalDate.now());
            enr.setActive(true);
            enr = enrollmentRepository.save(enr);
            Attendance a = new Attendance();
            a.setClassSession(session);
            a.setEnrollment(enr);
            a.setStatus(AttendanceStatus.ABSENT);
            a = attendanceRepository.save(a);
            MakeupRequest mr = new MakeupRequest();
            mr.setAttendance(a);
            mr.setStatus(MakeupRequestStatus.REQUESTED);
            mr.setReason("Test " + i);
            mr.setRequestedAt(LocalDateTime.now());
            makeupRequestRepository.save(mr);
        }

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

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getAlerts().size()).isLessThanOrEqualTo(5);
    }

    @Test
    void instrutorIdNasSessoes() {
        createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getUpcomingSessions()).isNotEmpty();
        assertThat(response.getUpcomingSessions().get(0).getInstructorId()).isEqualTo(instructor.getId());
    }

    @Test
    void classGroupIdNasReposicoes() {
        ClassSession session = createSession(LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        Attendance attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.ABSENT);
        attendance = attendanceRepository.save(attendance);

        MakeupRequest mr = new MakeupRequest();
        mr.setAttendance(attendance);
        mr.setStatus(MakeupRequestStatus.REQUESTED);
        mr.setReason("Teste");
        mr.setRequestedAt(LocalDateTime.now());
        makeupRequestRepository.save(mr);

        var response = dashboardOperationalService.getOperationalDashboard(studio.getId());

        assertThat(response.getPendingMakeupRequests()).isNotEmpty();
        assertThat(response.getPendingMakeupRequests().get(0).getClassGroupId()).isEqualTo(classGroup.getId());
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
