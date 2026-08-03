package br.com.corely.enrollment;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.dto.EnrollmentRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ObjectMapper objectMapper;

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
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Enrollment enrollmentA;
    private Enrollment enrollmentB;

    @BeforeEach
    void setUp() {
        Studio studioA = studioRepository.save(createStudio("Studio A"));
        Studio studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN);

        Student studentA = createAndSaveStudent(studioA, "Student A");
        Student studentB = createAndSaveStudent(studioB, "Student B");

        Instructor instructorA = createAndSaveInstructor(studioA, "Instructor A");
        Instructor instructorB = createAndSaveInstructor(studioB, "Instructor B");

        ClassGroup classGroupA = createAndSaveClassGroup(studioA, instructorA, "Class Group A");
        ClassGroup classGroupB = createAndSaveClassGroup(studioB, instructorB, "Class Group B");

        enrollmentA = createAndSaveEnrollment(studioA, studentA, classGroupA);
        enrollmentB = createAndSaveEnrollment(studioB, studentB, classGroupB);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnEnrollmentsFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_shouldReturn200_whenEnrollmentBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/enrollments/{id}", enrollmentA.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void findById_shouldReturn404_whenEnrollmentBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/enrollments/{id}", enrollmentB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn404_whenEnrollmentBelongsToOtherTenant() throws Exception {
        var request = new EnrollmentRequest();
        request.setStudentId(enrollmentB.getStudent().getId());
        request.setClassGroupId(enrollmentB.getClassGroup().getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        mockMvc.perform(put("/enrollments/{id}", enrollmentB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404_whenEnrollmentBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/enrollments/{id}", enrollmentB.getId()))
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
}
