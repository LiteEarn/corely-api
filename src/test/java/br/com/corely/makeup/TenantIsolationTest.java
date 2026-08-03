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
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private MakeupRequestRepository makeupRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Attendance attendanceA;
    private Attendance attendanceB;
    private MakeupRequest requestA;
    private MakeupRequest requestB;

    @BeforeEach
    void setUp() {
        Studio studioA = studioRepository.save(createStudio("Studio A"));
        Studio studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN);

        attendanceA = createAttendanceForStudio(studioA, "Studio A");
        attendanceB = createAttendanceForStudio(studioB, "Studio B");

        requestA = createAndSaveMakeupRequest(attendanceA, "Request A");
        requestB = createAndSaveMakeupRequest(attendanceB, "Request B");

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnRequestsFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/makeup-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findByAttendance_shouldReturn200_whenAttendanceBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/attendance/{attendanceId}/makeup-request", attendanceA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestA.getId().toString()));
    }

    @Test
    void findByAttendance_shouldReturn404_whenAttendanceBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/attendance/{attendanceId}/makeup-request", attendanceB.getId()))
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

    private Attendance createAttendanceForStudio(Studio studio, String prefix) {
        Student student = createAndSaveStudent(studio, prefix + " Student");
        Instructor instructor = createAndSaveInstructor(studio, prefix + " Instructor");
        ClassGroup classGroup = createAndSaveClassGroup(studio, instructor, prefix + " Class Group");
        Enrollment enrollment = createAndSaveEnrollment(studio, student, classGroup);
        ClassSession session = createAndSaveSession(classGroup, instructor, LocalDate.now().plusDays(7));

        var attendance = new Attendance();
        attendance.setClassSession(session);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.ABSENT);
        return attendanceRepository.save(attendance);
    }

    private MakeupRequest createAndSaveMakeupRequest(Attendance attendance, String reason) {
        var request = new MakeupRequest();
        request.setAttendance(attendance);
        request.setStatus(MakeupRequestStatus.REQUESTED);
        request.setReason(reason);
        request.setRequestedAt(LocalDateTime.now());
        return makeupRequestRepository.save(request);
    }
}
