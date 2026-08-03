package br.com.corely.attendance;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.enrollment.EnrollmentStatus;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private ClassGroup classGroupA;
    private ClassGroup classGroupB;
    private ClassSession sessionA;
    private ClassSession sessionB;

    @BeforeEach
    void setUp() {
        Studio studioA = studioRepository.save(createStudio("Studio A"));
        Studio studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN);

        Student studentA = createAndSaveStudent(studioA, "Student A");
        Student studentB = createAndSaveStudent(studioB, "Student B");

        Instructor instructorA = createAndSaveInstructor(studioA, "Instructor A");
        Instructor instructorB = createAndSaveInstructor(studioB, "Instructor B");

        classGroupA = createAndSaveClassGroup(studioA, instructorA, "Class Group A");
        classGroupB = createAndSaveClassGroup(studioB, instructorB, "Class Group B");

        Enrollment enrollmentA = createAndSaveEnrollment(studioA, studentA, classGroupA);
        Enrollment enrollmentB = createAndSaveEnrollment(studioB, studentB, classGroupB);

        sessionA = createAndSaveSession(classGroupA, instructorA, LocalDate.now().plusDays(7));
        sessionB = createAndSaveSession(classGroupB, instructorB, LocalDate.now().plusDays(7));

        createAndSaveAttendance(sessionA, enrollmentA, AttendanceStatus.PRESENT);
        createAndSaveAttendance(sessionB, enrollmentB, AttendanceStatus.PRESENT);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findBySession_shouldReturn200_whenSessionBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/class-sessions/{sessionId}/attendance", sessionA.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void findBySession_shouldReturn404_whenSessionBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/class-sessions/{sessionId}/attendance", sessionB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByClassGroupAndDate_shouldReturn404_whenClassGroupBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/attendance/class-group/{classGroupId}/date/{date}",
                        classGroupB.getId(), LocalDate.now().plusDays(7)))
                .andExpect(status().isNotFound());
    }

    private User createAndAuthenticateUser(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail("admin_a@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user;
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Student createAndSaveStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private Instructor createAndSaveInstructor(Studio studio, String name) {
        var instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName(name);
        instructor.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        instructor.setActive(true);
        return instructorRepository.save(instructor);
    }

    private ClassGroup createAndSaveClassGroup(Studio studio, Instructor instructor, String name) {
        var classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName(name);
        classGroup.setStartTime(LocalTime.of(8, 0));
        classGroup.setEndTime(LocalTime.of(9, 0));
        classGroup.setCapacity(10);
        classGroup.setActive(true);
        return classGroupRepository.save(classGroup);
    }

    private Enrollment createAndSaveEnrollment(Studio studio, Student student, ClassGroup classGroup) {
        var enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollmentRepository.save(enrollment);
    }

    private ClassSession createAndSaveSession(ClassGroup classGroup, Instructor instructor, LocalDate date) {
        var session = new ClassSession();
        session.setClassGroup(classGroup);
        session.setInstructor(instructor);
        session.setSessionDate(date);
        session.setStartTime(classGroup.getStartTime());
        session.setEndTime(classGroup.getEndTime());
        session.setStatus(ClassSessionStatus.SCHEDULED);
        return classSessionRepository.save(session);
    }

    private void createAndSaveAttendance(ClassSession session, Enrollment enrollment, AttendanceStatus status) {
        var attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(status);
        attendanceRepository.save(attendance);
    }
}
