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
import java.util.List;
import java.util.UUID;

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
                .hasMessage("The selected class group is inactive.");
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

    @Test
    void findByClassGroupIdAndAttendanceDate_returnsStudentsWhenNoAttendanceRecords() {
        // Ensure no attendance records exist
        attendanceRepository.deleteAll();

        LocalDate date = LocalDate.now();
        var students = attendanceService.findByClassGroupIdAndAttendanceDate(classGroup.getId(), date.toString());

        assertThat(students).isNotNull();
        assertThat(students).hasSize(1);
        assertThat(students.get(0).getStudentId()).isEqualTo(student.getId());
        assertThat(students.get(0).getStudentName()).isEqualTo("Test Student");
        assertThat(students.get(0).getClassGroupId()).isEqualTo(classGroup.getId());
        assertThat(students.get(0).getAttendanceDate()).isEqualTo(date);
        assertThat(students.get(0).getPresent()).isFalse(); // Default value
        assertThat(students.get(0).getId()).isNull(); // No attendance record yet
        assertThat(students.get(0).getNotes()).isNull();
    }

    @Test
    void findByClassGroupIdAndAttendanceDate_returnsExistingAttendanceWhenRecorded() {
        // Create an attendance record
        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                "Present today"
        );
        attendanceService.create(request);

        var students = attendanceService.findByClassGroupIdAndAttendanceDate(classGroup.getId(), LocalDate.now().toString());

        assertThat(students).isNotNull();
        assertThat(students).hasSize(1);
        assertThat(students.get(0).getStudentId()).isEqualTo(student.getId());
        assertThat(students.get(0).getPresent()).isTrue();
        assertThat(students.get(0).getNotes()).isEqualTo("Present today");
        assertThat(students.get(0).getId()).isNotNull(); // Attendance record exists
    }

    @Test
    void findByClassGroupIdAndAttendanceDate_excludesInactiveStudents() {
        // Create an inactive student with an active enrollment
        Student inactiveStudent = new Student();
        inactiveStudent.setStudio(studio);
        inactiveStudent.setFullName("Inactive Student");
        inactiveStudent.setEmail("inactive@test.com");
        inactiveStudent.setActive(false);
        inactiveStudent = studentRepository.save(inactiveStudent);

        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setStudio(studio);
        inactiveEnrollment.setStudent(inactiveStudent);
        inactiveEnrollment.setClassGroup(classGroup);
        inactiveEnrollment.setEnrollmentDate(LocalDate.now());
        inactiveEnrollment.setActive(true);
        inactiveEnrollment = enrollmentRepository.save(inactiveEnrollment);

        var students = attendanceService.findByClassGroupIdAndAttendanceDate(classGroup.getId(), LocalDate.now().toString());

        assertThat(students).hasSize(1); // Only active student
        assertThat(students.get(0).getStudentId()).isEqualTo(student.getId());
    }

    @Test
    void findByClassGroupIdAndAttendanceDate_excludesInactiveEnrollments() {
        // Create an active student with an inactive enrollment
        Student activeStudent2 = new Student();
        activeStudent2.setStudio(studio);
        activeStudent2.setFullName("Active Student 2");
        activeStudent2.setEmail("active2@test.com");
        activeStudent2.setActive(true);
        activeStudent2 = studentRepository.save(activeStudent2);

        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setStudio(studio);
        inactiveEnrollment.setStudent(activeStudent2);
        inactiveEnrollment.setClassGroup(classGroup);
        inactiveEnrollment.setEnrollmentDate(LocalDate.now());
        inactiveEnrollment.setActive(false);
        inactiveEnrollment = enrollmentRepository.save(inactiveEnrollment);

        var students = attendanceService.findByClassGroupIdAndAttendanceDate(classGroup.getId(), LocalDate.now().toString());

        assertThat(students).hasSize(1); // Only student with active enrollment
        assertThat(students.get(0).getStudentId()).isEqualTo(student.getId());
    }

    @Test
    void findByClassGroupIdAndAttendanceDate_excludesInactiveClassGroups() {
        // Create an inactive class group with active enrollment
        ClassGroup inactiveClassGroup = new ClassGroup();
        inactiveClassGroup.setStudio(studio);
        inactiveClassGroup.setInstructor(instructor);
        inactiveClassGroup.setName("Inactive Class Group");
        inactiveClassGroup.setStartTime(LocalTime.of(14, 0));
        inactiveClassGroup.setEndTime(LocalTime.of(15, 0));
        inactiveClassGroup.setCapacity(10);
        inactiveClassGroup.setMonday(true);
        inactiveClassGroup.setActive(false);
        inactiveClassGroup = classGroupRepository.save(inactiveClassGroup);

        Student activeStudent2 = new Student();
        activeStudent2.setStudio(studio);
        activeStudent2.setFullName("Active Student 2");
        activeStudent2.setEmail("active2@test.com");
        activeStudent2.setActive(true);
        activeStudent2 = studentRepository.save(activeStudent2);

        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setStudio(studio);
        activeEnrollment.setStudent(activeStudent2);
        activeEnrollment.setClassGroup(inactiveClassGroup);
        activeEnrollment.setEnrollmentDate(LocalDate.now());
        activeEnrollment.setActive(true);
        activeEnrollment = enrollmentRepository.save(activeEnrollment);

        // Query for the inactive class group should return empty
        var students = attendanceService.findByClassGroupIdAndAttendanceDate(inactiveClassGroup.getId(), LocalDate.now().toString());

        assertThat(students).isEmpty(); // Inactive class group excluded
    }

    @Test
    void findByClassGroupIdAndAttendanceDate_returnsEmptyListWhenNoActiveEnrollments() {
        // Remove all enrollments
        enrollmentRepository.deleteAll();

        var students = attendanceService.findByClassGroupIdAndAttendanceDate(classGroup.getId(), LocalDate.now().toString());

        assertThat(students).isEmpty();
    }

    @Test
    void findByClassGroupIdAndAttendanceDate_returnsMultipleStudentsWithMixedAttendance() {
        // Capture student IDs as final variables for use in lambda
        final java.util.UUID student1Id = student.getId();

        // Add another active student with enrollment
        Student student2 = new Student();
        student2.setStudio(studio);
        student2.setFullName("Student 2");
        student2.setEmail("student2@test.com");
        student2.setActive(true);
        final Student savedStudent2 = studentRepository.save(student2);

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudio(studio);
        enrollment2.setStudent(savedStudent2);
        enrollment2.setClassGroup(classGroup);
        enrollment2.setEnrollmentDate(LocalDate.now());
        enrollment2.setActive(true);
        enrollmentRepository.save(enrollment2);

        // Create attendance for first student only
        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student1Id,
                classGroup.getId(),
                LocalDate.now(),
                true,
                "Present"
        );
        attendanceService.create(request);

        var students = attendanceService.findByClassGroupIdAndAttendanceDate(classGroup.getId(), LocalDate.now().toString());

        assertThat(students).hasSize(2);

        // First student has attendance record
        var studentWithAttendance = students.stream()
                .filter(s -> s.getStudentId().equals(student1Id))
                .findFirst()
                .orElseThrow();
        assertThat(studentWithAttendance.getPresent()).isTrue();
        assertThat(studentWithAttendance.getId()).isNotNull();

        // Second student has no attendance record
        var studentWithoutAttendance = students.stream()
                .filter(s -> s.getStudentId().equals(savedStudent2.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(studentWithoutAttendance.getPresent()).isFalse();
        assertThat(studentWithoutAttendance.getId()).isNull();
    }

    @Test
    void createBulk_updatesExistingAttendance() {
        // Create initial attendance
        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                "Initial note"
        );
        var initialAttendance = attendanceService.create(request);

        // Now use bulk to update the same student's attendance
        var bulkRequest = new br.com.corely.attendance.dto.AttendanceBulkRequest(
                LocalDate.now(),
                classGroup.getId(),
                studio.getId(),
                List.of(new br.com.corely.attendance.dto.AttendanceItemRequest(
                        student.getId(),
                        false,
                        "Updated note"
                ))
        );

        var updatedAttendances = attendanceService.createBulk(bulkRequest);

        assertThat(updatedAttendances).hasSize(1);
        assertThat(updatedAttendances.get(0).getId()).isEqualTo(initialAttendance.getId()); // Same ID
        assertThat(updatedAttendances.get(0).getPresent()).isFalse(); // Updated to absent
        assertThat(updatedAttendances.get(0).getNotes()).isEqualTo("Updated note"); // Updated notes
    }

    @Test
    void createBulk_createsNewAndUpdatesExisting() {
        // Create initial attendance for student 1
        AttendanceRequest request = new AttendanceRequest(
                studio.getId(),
                student.getId(),
                classGroup.getId(),
                LocalDate.now(),
                true,
                "Present"
        );
        var initialAttendance = attendanceService.create(request);

        // Add another student
        Student student2 = new Student();
        student2.setStudio(studio);
        student2.setFullName("Student 2");
        student2.setEmail("student2@test.com");
        student2.setActive(true);
        student2 = studentRepository.save(student2);
        final UUID student2Id = student2.getId();

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudio(studio);
        enrollment2.setStudent(student2);
        enrollment2.setClassGroup(classGroup);
        enrollment2.setEnrollmentDate(LocalDate.now());
        enrollment2.setActive(true);
        enrollmentRepository.save(enrollment2);

        // Bulk save: update student 1, create for student 2
        var bulkRequest = new br.com.corely.attendance.dto.AttendanceBulkRequest(
                LocalDate.now(),
                classGroup.getId(),
                studio.getId(),
                List.of(
                        new br.com.corely.attendance.dto.AttendanceItemRequest(
                                student.getId(),
                                false,
                                "Updated to absent"
                        ),
                        new br.com.corely.attendance.dto.AttendanceItemRequest(
                                student2Id,
                                true,
                                "Present"
                        )
                )
        );

        var results = attendanceService.createBulk(bulkRequest);

        assertThat(results).hasSize(2);

        // Student 1: updated
        var student1Result = results.stream()
                .filter(r -> r.getStudentId().equals(student.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(student1Result.getId()).isEqualTo(initialAttendance.getId());
        assertThat(student1Result.getPresent()).isFalse();
        assertThat(student1Result.getNotes()).isEqualTo("Updated to absent");

        // Student 2: created
        var student2Result = results.stream()
                .filter(r -> r.getStudentId().equals(student2Id))
                .findFirst()
                .orElseThrow();
        assertThat(student2Result.getId()).isNotNull();
        assertThat(student2Result.getPresent()).isTrue();
        assertThat(student2Result.getNotes()).isEqualTo("Present");
    }
}
