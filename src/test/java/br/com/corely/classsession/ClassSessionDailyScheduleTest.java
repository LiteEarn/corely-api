package br.com.corely.classsession;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.dto.DailyScheduleResponse;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClassSessionDailyScheduleTest {

    @Autowired
    private ClassSessionService classSessionService;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private ClassGroup otherClassGroup;
    private Student student;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        classSessionRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Instrutor Teste");
        instructor.setEmail("instrutor@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName("Ballet Infantil");
        classGroup.setStartTime(LocalTime.of(8, 0));
        classGroup.setEndTime(LocalTime.of(9, 0));
        classGroup.setCapacity(20);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        classGroup = classGroupRepository.save(classGroup);

        otherClassGroup = new ClassGroup();
        otherClassGroup.setStudio(studio);
        otherClassGroup.setInstructor(instructor);
        otherClassGroup.setName("Jazz Adulto");
        otherClassGroup.setStartTime(LocalTime.of(10, 0));
        otherClassGroup.setEndTime(LocalTime.of(11, 0));
        otherClassGroup.setCapacity(15);
        otherClassGroup.setMonday(true);
        otherClassGroup.setActive(true);
        otherClassGroup = classGroupRepository.save(otherClassGroup);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Aluno Teste");
        student.setActive(true);
        student = studentRepository.save(student);

        enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        User user = new User();
        user.setStudio(studio);
        user.setEmail("admin@test.com");
        user.setPassword("pass");
        user.setRole(UserRole.ADMIN);
        user.setName("Admin");
        user.setActive(true);
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getDailySchedule_whenNoFilters_returnsAllDaySessionsWithKpis() {
        ClassSession session1 = createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.SCHEDULED);
        ClassSession session2 = createSession(otherClassGroup, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getKpis().getTotalToday()).isEqualTo(2);
        assertThat(response.getKpis().getInProgress()).isZero();
        assertThat(response.getKpis().getCompleted()).isZero();
        assertThat(response.getKpis().getCancelled()).isZero();
        assertThat(response.getSessions()).hasSize(2);
        assertThat(response.getSessions().get(0).getClassGroupName()).isEqualTo("Ballet Infantil");
        assertThat(response.getSessions().get(1).getClassGroupName()).isEqualTo("Jazz Adulto");
    }

    @Test
    void getDailySchedule_withInstructorFilter_returnsFilteredSessions() {
        createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.SCHEDULED);
        createSession(otherClassGroup, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), instructor.getId(), null, null);

        assertThat(response.getSessions()).hasSize(2);
        assertThat(response.getKpis().getTotalToday()).isEqualTo(2);
    }

    @Test
    void getDailySchedule_withClassGroupFilter_returnsFilteredSessions() {
        createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.SCHEDULED);
        createSession(otherClassGroup, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, null, classGroup.getId());

        assertThat(response.getSessions()).hasSize(1);
        assertThat(response.getSessions().get(0).getClassGroupId()).isEqualTo(classGroup.getId());
    }

    @Test
    void getDailySchedule_withStatusFilter_returnsFilteredSessions() {
        createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.IN_PROGRESS);
        createSession(otherClassGroup, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, ClassSessionStatus.IN_PROGRESS, null);

        assertThat(response.getSessions()).hasSize(1);
        assertThat(response.getSessions().get(0).getStatus()).isEqualTo(ClassSessionStatus.IN_PROGRESS);
    }

    @Test
    void getDailySchedule_kpis_reflectStatusCounts() {
        createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.IN_PROGRESS);
        createSession(otherClassGroup, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);
        ClassSession completed = createSession(classGroup, LocalDate.now(), LocalTime.of(14, 0), LocalTime.of(15, 0), ClassSessionStatus.COMPLETED);
        ClassSession cancelled = createSession(otherClassGroup, LocalDate.now(), LocalTime.of(16, 0), LocalTime.of(17, 0), ClassSessionStatus.CANCELLED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, null, null);

        assertThat(response.getKpis().getTotalToday()).isEqualTo(4);
        assertThat(response.getKpis().getInProgress()).isEqualTo(1);
        assertThat(response.getKpis().getCompleted()).isEqualTo(1);
        assertThat(response.getKpis().getCancelled()).isEqualTo(1);
    }

    @Test
    void getDailySchedule_includesEnrolledAndPresentCounts() {
        ClassSession session = createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.IN_PROGRESS);

        Attendance attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(attendance);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, null, null);

        assertThat(response.getSessions()).hasSize(1);
        DailyScheduleResponse.DailySessionItem item = response.getSessions().get(0);
        assertThat(item.getCapacity()).isEqualTo(20);
        assertThat(item.getEnrolledCount()).isEqualTo(1);
        assertThat(item.getPresentCount()).isEqualTo(1);
    }

    @Test
    void getDailySchedule_whenNoSessions_returnsEmptyWithZeroKpis() {
        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, null, null);

        assertThat(response.getKpis().getTotalToday()).isZero();
        assertThat(response.getSessions()).isEmpty();
    }

    @Test
    void getDailySchedule_sessionsOrderedByStartTime() {
        createSession(classGroup, LocalDate.now(), LocalTime.of(16, 0), LocalTime.of(17, 0), ClassSessionStatus.SCHEDULED);
        createSession(otherClassGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.SCHEDULED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), LocalDate.now(), null, null, null);

        assertThat(response.getSessions()).hasSize(2);
        assertThat(response.getSessions().get(0).getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.getSessions().get(1).getStartTime()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void getDailySchedule_withDifferentDate_returnsSessionsForThatDate() {
        createSession(classGroup, LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), ClassSessionStatus.SCHEDULED);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ClassSession tomorrowSession = createSession(otherClassGroup, tomorrow, LocalTime.of(10, 0), LocalTime.of(11, 0), ClassSessionStatus.SCHEDULED);

        DailyScheduleResponse response = classSessionService.getDailySchedule(studio.getId(), tomorrow, null, null, null);

        assertThat(response.getSessions()).hasSize(1);
        assertThat(response.getSessions().get(0).getId()).isEqualTo(tomorrowSession.getId());
    }

    private ClassSession createSession(ClassGroup cg, LocalDate date, LocalTime start, LocalTime end, ClassSessionStatus status) {
        ClassSession session = new ClassSession();
        session.setClassGroup(cg);
        session.setInstructor(cg.getInstructor());
        session.setSessionDate(date);
        session.setStartTime(start);
        session.setEndTime(end);
        session.setStatus(status);
        return classSessionRepository.save(session);
    }
}