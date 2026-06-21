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
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private Studio studio;
    private Student activeStudent;
    private Student inactiveStudent;
    private ClassGroup classGroup1;
    private ClassGroup classGroup2;

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

        activeStudent = createStudent("Active Student", "active@test.com", true);
        inactiveStudent = createStudent("Inactive Student", "inactive@test.com", false);

        Instructor instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup1 = createClassGroup(instructor, "Class 1");
        classGroup2 = createClassGroup(instructor, "Class 2");
    }

    @Test
    void create_activeStudent_succeeds() {
        EnrollmentRequest request = buildRequest(activeStudent.getId(), classGroup1.getId());

        EnrollmentResponse response = enrollmentService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(activeStudent.getId());
        assertThat(response.getActive()).isTrue();
        assertThat(enrollmentRepository.findById(response.getId())).isPresent();
    }

    @Test
    void create_inactiveStudent_throwsBusinessException() {
        EnrollmentRequest request = buildRequest(inactiveStudent.getId(), classGroup1.getId());

        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível matricular um aluno inativo.");

        assertThat(enrollmentRepository.findByStudentIdAndClassGroupId(inactiveStudent.getId(), classGroup1.getId()))
                .isEmpty();
    }

    @Test
    void create_inactiveStudent_doesNotAffectExistingEnrollments() {
        EnrollmentResponse existing = enrollmentService.create(buildRequest(activeStudent.getId(), classGroup1.getId()));

        assertThatThrownBy(() -> enrollmentService.create(buildRequest(inactiveStudent.getId(), classGroup2.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível matricular um aluno inativo.");

        assertThat(enrollmentRepository.findAll()).hasSize(1);
        assertThat(enrollmentRepository.findById(existing.getId()).orElseThrow().getActive()).isTrue();
    }

    private Student createStudent(String name, String email, boolean active) {
        Student student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setEmail(email);
        student.setActive(active);
        return studentRepository.save(student);
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

    private EnrollmentRequest buildRequest(UUID studentId, UUID classGroupId) {
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(studentId);
        request.setClassGroupId(classGroupId);
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);
        return request;
    }
}
