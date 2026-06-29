package br.com.corely.enrollment;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.dto.EnrollmentRequest;
import br.com.corely.enrollment.dto.EnrollmentResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
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
    void update_whenClassGroupInactive_throwsConflictException() {
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
                .isInstanceOf(ConflictException.class)
                .hasMessage("Turma inativa");
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

    @Test
    void update_whenMovingToInactiveClassGroup_throwsConflictException() {
        // Given - create an inactive class group
        ClassGroup inactiveClassGroup = new ClassGroup();
        inactiveClassGroup.setStudio(studio);
        inactiveClassGroup.setInstructor(instructor);
        inactiveClassGroup.setName("Inactive Class Group");
        inactiveClassGroup.setStartTime(LocalTime.of(14, 0));
        inactiveClassGroup.setEndTime(LocalTime.of(15, 0));
        inactiveClassGroup.setCapacity(10);
        inactiveClassGroup.setTuesday(true);
        inactiveClassGroup.setActive(false);
        inactiveClassGroup = classGroupRepository.save(inactiveClassGroup);

        // Given - create an enrollment in the active class group
        Enrollment newEnrollment = new Enrollment();
        newEnrollment.setStudio(studio);
        newEnrollment.setStudent(student);
        newEnrollment.setClassGroup(classGroup);
        newEnrollment.setEnrollmentDate(LocalDate.now());
        newEnrollment.setActive(true);
        Enrollment enrollment = enrollmentRepository.save(newEnrollment);

        // Given - try to move enrollment to inactive class group
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(inactiveClassGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.update(enrollment.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Turma inativa");
    }

    @Test
    void create_whenStudentNotFound_throwsResourceNotFoundException() {
        // Given
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(UUID.randomUUID());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aluno inexistente");
    }

    @Test
    void create_whenClassGroupNotFound_throwsResourceNotFoundException() {
        // Given
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(UUID.randomUUID());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Turma inexistente");
    }

    @Test
    void create_whenClassGroupFull_throwsConflictException() {
        // Given - set capacity to 1
        classGroup.setCapacity(1);
        classGroupRepository.save(classGroup);

        // Given - create an enrollment
        Enrollment existingEnrollment = new Enrollment();
        existingEnrollment.setStudio(studio);
        existingEnrollment.setStudent(student);
        existingEnrollment.setClassGroup(classGroup);
        existingEnrollment.setEnrollmentDate(LocalDate.now());
        existingEnrollment.setActive(true);
        enrollmentRepository.save(existingEnrollment);

        // Given - create another student
        Student anotherStudent = new Student();
        anotherStudent.setStudio(studio);
        anotherStudent.setFullName("Another Student");
        anotherStudent.setEmail("another@test.com");
        anotherStudent.setActive(true);
        anotherStudent = studentRepository.save(anotherStudent);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(anotherStudent.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Turma cheia");
    }

    @Test
    void create_whenStudentAlreadyEnrolled_throwsConflictException() {
        // Given - create an enrollment
        Enrollment existingEnrollment = new Enrollment();
        existingEnrollment.setStudio(studio);
        existingEnrollment.setStudent(student);
        existingEnrollment.setClassGroup(classGroup);
        existingEnrollment.setEnrollmentDate(LocalDate.now());
        existingEnrollment.setActive(true);
        enrollmentRepository.save(existingEnrollment);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Aluno já matriculado");
    }

    @Test
    void create_whenStudentInactive_throwsConflictException() {
        // Given - deactivate student
        student.setActive(false);
        studentRepository.save(student);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Aluno inativo");
    }

    @Test
    void create_whenClassGroupInactive_throwsConflictException() {
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
                .isInstanceOf(ConflictException.class)
                .hasMessage("Turma inativa");
    }

    @Test
    void create_whenInstructorInactive_throwsConflictException() {
        // Given - deactivate instructor
        instructor.setActive(false);
        instructorRepository.save(instructor);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Instrutor inativo");
    }

    @Test
    void create_whenEnrollmentDateFuture_throwsBusinessException() {
        // Given
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now().plusDays(1));
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Data de matrícula não pode ser futura");
    }

    @Test
    void delete_whenEnrollmentExists_succeeds() {
        // Given - create an enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        // When
        enrollmentService.delete(enrollment.getId());

        // Then
        assertThat(enrollmentRepository.findById(enrollment.getId())).isEmpty();
    }

    @Test
    void delete_whenEnrollmentNotFound_throwsResourceNotFoundException() {
        // When & Then
        assertThatThrownBy(() -> enrollmentService.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Matrícula inexistente");
    }

    @Test
    void update_whenEnrollmentExists_succeeds() {
        // Given - create an enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now().minusDays(1));
        request.setActive(false);

        // When
        EnrollmentResponse response = enrollmentService.update(enrollment.getId(), request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEnrollmentDate()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void update_whenStudentInactive_throwsConflictException() {
        // Given - create an enrollment
        Enrollment newEnrollment = new Enrollment();
        newEnrollment.setStudio(studio);
        newEnrollment.setStudent(student);
        newEnrollment.setClassGroup(classGroup);
        newEnrollment.setEnrollmentDate(LocalDate.now());
        newEnrollment.setActive(true);
        UUID enrollmentId = enrollmentRepository.save(newEnrollment).getId();

        // Given - deactivate student
        student.setActive(false);
        studentRepository.save(student);

        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(classGroup.getId());
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.update(enrollmentId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Aluno inativo");
    }

    @Test
    void update_whenClassGroupFull_throwsConflictException() {
        // Given - create a second class group with capacity 1
        ClassGroup targetClassGroup = new ClassGroup();
        targetClassGroup.setStudio(studio);
        targetClassGroup.setInstructor(instructor);
        targetClassGroup.setName("Target Class Group");
        targetClassGroup.setStartTime(LocalTime.of(14, 0));
        targetClassGroup.setEndTime(LocalTime.of(15, 0));
        targetClassGroup.setCapacity(1);
        targetClassGroup.setTuesday(true);
        targetClassGroup.setActive(true);
        UUID targetClassGroupId = classGroupRepository.save(targetClassGroup).getId();

        // Given - create another student and fill the target class group
        Student anotherStudent = new Student();
        anotherStudent.setStudio(studio);
        anotherStudent.setFullName("Another Student");
        anotherStudent.setEmail("another@test.com");
        anotherStudent.setActive(true);
        UUID anotherStudentId = studentRepository.save(anotherStudent).getId();

        Enrollment existingEnrollment = new Enrollment();
        existingEnrollment.setStudio(studio);
        existingEnrollment.setStudent(studentRepository.findById(anotherStudentId).orElseThrow());
        existingEnrollment.setClassGroup(classGroupRepository.findById(targetClassGroupId).orElseThrow());
        existingEnrollment.setEnrollmentDate(LocalDate.now());
        existingEnrollment.setActive(true);
        enrollmentRepository.save(existingEnrollment);

        // Given - create an enrollment in the original class group
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        UUID enrollmentId = enrollmentRepository.save(enrollment).getId();

        // Given - try to move enrollment to the full class group
        EnrollmentRequest request = new EnrollmentRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setClassGroupId(targetClassGroupId);
        request.setEnrollmentDate(LocalDate.now());
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.update(enrollmentId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Turma cheia");
    }

}
