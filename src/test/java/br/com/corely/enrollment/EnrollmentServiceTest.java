package br.com.corely.enrollment;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.dto.EnrollmentRequest;
import br.com.corely.enrollment.dto.EnrollmentResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private Student student;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
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
    }

    @Test
    void findStudentsByClassGroupId_returnsStudentsWhenAllAreActive() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> responses = enrollmentService.findStudentsByClassGroupId(classGroup.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStudentName()).isEqualTo("Test Student");
    }

    @Test
    void findStudentsByClassGroupId_excludesInactiveStudents() {
        student.setActive(false);
        studentRepository.save(student);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> responses = enrollmentService.findStudentsByClassGroupId(classGroup.getId());

        assertThat(responses).isEmpty();
    }

    @Test
    void findStudentsByClassGroupId_excludesInactiveEnrollments() {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> responses = enrollmentService.findStudentsByClassGroupId(classGroup.getId());

        assertThat(responses).isEmpty();
    }

    @Test
    void findStudentsByClassGroupId_excludesWhenClassGroupIsInactive() {
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollmentRepository.save(enrollment);

        List<EnrollmentResponse> responses = enrollmentService.findStudentsByClassGroupId(classGroup.getId());

        assertThat(responses).isEmpty();
    }

    @Test
    void create_whenClassGroupInactive_throwsBusinessException() {
        // Given - deactivate class group
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot enroll a student in an inactive class group.");
    }

    @Test
    void update_whenClassGroupInactive_throwsBusinessException() {
        // Given - create an enrollment
        Enrollment newEnrollment = new Enrollment();
        newEnrollment.setStudio(studio);
        newEnrollment.setStudent(student);
        newEnrollment.setClassGroup(classGroup);
        newEnrollment.setEnrollmentDate(LocalDate.now());
        newEnrollment.setActive(true);
        Enrollment enrollment = enrollmentRepository.save(newEnrollment);

        // Given - deactivate class group
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.update(enrollment.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot enroll a student in an inactive class group.");
    }

    @Test
    void create_whenClassGroupActive_succeeds() {
        // Given
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When
        EnrollmentResponse response = enrollmentService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(student.getId());
        assertThat(response.getClassGroupId()).isEqualTo(classGroup.getId());
    }
}
