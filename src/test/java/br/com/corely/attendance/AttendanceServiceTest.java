package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;

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

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private Student student;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
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

        enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);
    }

    @Test
    void create_attendanceSavedWhenAllActive() {
        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                "Test notes"
        );

        var response = attendanceService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(student.getId());
        assertThat(response.getClassGroupId()).isEqualTo(classGroup.getId());
        assertThat(response.getPresent()).isTrue();
        assertThat(response.getNotes()).isEqualTo("Test notes");
    }

    @Test
    void create_throwsExceptionWhenStudentInactive() {
        student.setActive(false);
        studentRepository.save(student);

        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                null
        );

        assertThatThrownBy(() -> attendanceService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Aluno inativo.");
    }

    @Test
    void create_throwsExceptionWhenEnrollmentInactive() {
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                null
        );

        assertThatThrownBy(() -> attendanceService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Matrícula inativa.");
    }

    @Test
    void create_throwsExceptionWhenClassGroupInactive() {
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                null
        );

        assertThatThrownBy(() -> attendanceService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Turma inativa.");
    }

    @Test
    void historicalAttendanceRemainsUnchanged() {
        // Create attendance with all active entities
        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now().minusDays(1),
                true,
                "Historical record"
        );

        var savedAttendance = attendanceService.create(request);

        // Deactivate student after attendance was recorded
        student.setActive(false);
        studentRepository.save(student);

        // Retrieve the historical attendance - should still exist unchanged
        var retrieved = attendanceService.findById(savedAttendance.getId());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(savedAttendance.getId());
        assertThat(retrieved.getNotes()).isEqualTo("Historical record");
        assertThat(retrieved.getPresent()).isTrue();
    }
}
