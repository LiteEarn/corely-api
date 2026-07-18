package br.com.corely.student;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.finance.membershipplan.MembershipPlan;
import br.com.corely.finance.membershipplan.MembershipPlanRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.dto.StudentRequest;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Studio otherStudio;
    private MembershipPlan plan;
    private MembershipPlan otherPlan;
    private Student student;
    private ClassGroup classGroup1;
    private ClassGroup classGroup2;
    private ClassGroup classGroup3;
    private Enrollment activeEnrollment1;
    private Enrollment activeEnrollment2;
    private Enrollment inactiveEnrollment;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        membershipPlanRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        otherStudio = new Studio();
        otherStudio.setName("Other Studio");
        otherStudio = studioRepository.save(otherStudio);

        plan = new MembershipPlan();
        plan.setStudio(studio);
        plan.setName("Plano Teste");
        plan.setMonthlyPrice(BigDecimal.valueOf(199));
        plan.setSessionsPerWeek(2);
        plan.setActive(true);
        plan = membershipPlanRepository.save(plan);

        otherPlan = new MembershipPlan();
        otherPlan.setStudio(otherStudio);
        otherPlan.setName("Plano do Outro Studio");
        otherPlan.setMonthlyPrice(BigDecimal.valueOf(299));
        otherPlan.setSessionsPerWeek(3);
        otherPlan.setActive(true);
        otherPlan = membershipPlanRepository.save(otherPlan);

        authenticateAs(studio, UserRole.ADMIN);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setEmail("student@test.com");
        student.setActive(true);
        student = studentRepository.save(student);

        Instructor instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup1 = createClassGroup(instructor, "Class 1");
        classGroup2 = createClassGroup(instructor, "Class 2");
        classGroup3 = createClassGroup(instructor, "Class 3");

        activeEnrollment1 = createEnrollment(classGroup1, true);
        activeEnrollment2 = createEnrollment(classGroup2, true);
        inactiveEnrollment = createEnrollment(classGroup3, false);
    }

    @Test
    void deactivateStudent_deactivatesAllActiveEnrollments() {
        StudentRequest request = buildRequest(false);

        studentService.update(student.getId(), request);

        assertThat(enrollmentRepository.findById(activeEnrollment1.getId()).orElseThrow().getActive()).isFalse();
        assertThat(enrollmentRepository.findById(activeEnrollment2.getId()).orElseThrow().getActive()).isFalse();
        assertThat(enrollmentRepository.findById(inactiveEnrollment.getId()).orElseThrow().getActive()).isFalse();
        assertThat(studentRepository.findById(student.getId()).orElseThrow().getActive()).isFalse();
    }

    @Test
    void deactivateStudent_preservesEnrollmentRecords() {
        UUID enrollment1Id = activeEnrollment1.getId();
        UUID enrollment2Id = activeEnrollment2.getId();
        UUID inactiveEnrollmentId = inactiveEnrollment.getId();

        studentService.update(student.getId(), buildRequest(false));

        assertThat(enrollmentRepository.findById(enrollment1Id)).isPresent();
        assertThat(enrollmentRepository.findById(enrollment2Id)).isPresent();
        assertThat(enrollmentRepository.findById(inactiveEnrollmentId)).isPresent();
    }

    @Test
    void reactivateStudent_doesNotReactivateEnrollments() {
        studentService.update(student.getId(), buildRequest(false));

        studentService.update(student.getId(), buildRequest(true));

        assertThat(enrollmentRepository.findById(activeEnrollment1.getId()).orElseThrow().getActive()).isFalse();
        assertThat(enrollmentRepository.findById(activeEnrollment2.getId()).orElseThrow().getActive()).isFalse();
    }

    @Test
    void updateStudentWithoutStatusChange_leavesEnrollmentsUnchanged() {
        StudentRequest request = buildRequest(true);
        request.setFullName("Updated Name");

        studentService.update(student.getId(), request);

        assertThat(enrollmentRepository.findById(activeEnrollment1.getId()).orElseThrow().getActive()).isTrue();
        assertThat(enrollmentRepository.findById(activeEnrollment2.getId()).orElseThrow().getActive()).isTrue();
    }

    private ClassGroup createClassGroup(Instructor instructor, String name) {
        ClassGroup classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName(name);
        classGroup.setStartTime(LocalTime.of(10, 0));
        classGroup.setEndTime(LocalTime.of(11, 0));
        classGroup.setCapacity(10);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        return classGroupRepository.save(classGroup);
    }

    private Enrollment createEnrollment(ClassGroup classGroup, boolean active) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(active);
        return enrollmentRepository.save(enrollment);
    }

    @Test
    void create_shouldThrowException_whenPlanBelongsToOtherStudio() {
        authenticateAs(studio, UserRole.ADMIN);

        StudentRequest request = new StudentRequest();
        request.setStudioId(studio.getId());
        request.setFullName("New Student");
        request.setEmail("new@test.com");
        request.setActive(true);
        request.setMembershipPlanId(otherPlan.getId());

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Membership plan not found");
    }

    @Test
    void update_shouldThrowException_whenPlanBelongsToOtherStudio() {
        authenticateAs(studio, UserRole.ADMIN);

        StudentRequest request = new StudentRequest();
        request.setStudioId(studio.getId());
        request.setFullName(student.getFullName());
        request.setEmail(student.getEmail());
        request.setActive(true);
        request.setMembershipPlanId(otherPlan.getId());

        assertThatThrownBy(() -> studentService.update(student.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Membership plan not found");
    }

    private StudentRequest buildRequest(boolean active) {
        StudentRequest request = new StudentRequest();
        request.setStudioId(studio.getId());
        request.setFullName(student.getFullName());
        request.setEmail(student.getEmail());
        request.setActive(active);
        return request;
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
