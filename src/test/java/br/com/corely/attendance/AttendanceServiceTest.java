package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceBulkItem;
import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
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
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudioRepository studioRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private ClassGroup otherClassGroup;
    private Student student;
    private Enrollment enrollment;
    private ClassSession completedSession;
    private ClassSession inProgressSession;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        enrollmentRepository.deleteAll();
        classSessionRepository.deleteAll();
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

        otherClassGroup = new ClassGroup();
        otherClassGroup.setStudio(studio);
        otherClassGroup.setInstructor(instructor);
        otherClassGroup.setName("Other Class Group");
        otherClassGroup.setStartTime(LocalTime.of(14, 0));
        otherClassGroup.setEndTime(LocalTime.of(15, 0));
        otherClassGroup.setCapacity(10);
        otherClassGroup.setMonday(true);
        otherClassGroup.setActive(true);
        otherClassGroup = classGroupRepository.save(otherClassGroup);

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

        completedSession = new ClassSession();
        completedSession.setClassGroup(classGroup);
        completedSession.setInstructor(instructor);
        completedSession.setSessionDate(LocalDate.now());
        completedSession.setStartTime(LocalTime.of(10, 0));
        completedSession.setEndTime(LocalTime.of(11, 0));
        completedSession.setStatus(ClassSessionStatus.COMPLETED);
        completedSession = classSessionRepository.save(completedSession);

        inProgressSession = new ClassSession();
        inProgressSession.setClassGroup(classGroup);
        inProgressSession.setInstructor(instructor);
        inProgressSession.setSessionDate(LocalDate.now());
        inProgressSession.setStartTime(LocalTime.of(10, 0));
        inProgressSession.setEndTime(LocalTime.of(11, 0));
        inProgressSession.setStatus(ClassSessionStatus.IN_PROGRESS);
        inProgressSession = classSessionRepository.save(inProgressSession);
    }

    @Test
    void register_validAttendance() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                "Test notes"
        );

        var response = attendanceService.register(inProgressSession.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.classSessionId()).isEqualTo(inProgressSession.getId());
        assertThat(response.enrollmentId()).isEqualTo(enrollment.getId());
        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(response.notes()).isEqualTo("Test notes");
    }

    @Test
    void register_sessionNotFound() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );

        assertThatThrownBy(() -> attendanceService.register(java.util.UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Class session not found");
    }

    @Test
    void register_enrollmentNotFound() {
        AttendanceRequest request = new AttendanceRequest(
                java.util.UUID.randomUUID(),
                AttendanceStatus.PRESENT,
                null
        );

        assertThatThrownBy(() -> attendanceService.register(inProgressSession.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Enrollment not found");
    }

    @Test
    void register_sessionNotInProgress() {
        ClassSession scheduledSession = new ClassSession();
        scheduledSession.setClassGroup(classGroup);
        scheduledSession.setInstructor(instructor);
        scheduledSession.setSessionDate(LocalDate.now());
        scheduledSession.setStartTime(LocalTime.of(10, 0));
        scheduledSession.setEndTime(LocalTime.of(11, 0));
        scheduledSession.setStatus(ClassSessionStatus.SCHEDULED);
        ClassSession savedScheduled = classSessionRepository.save(scheduledSession);

        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );

        assertThatThrownBy(() -> attendanceService.register(savedScheduled.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A presença somente pode ser registrada durante a aula.");
    }

    @Test
    void register_inactiveEnrollment() {
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );

        assertThatThrownBy(() -> attendanceService.register(inProgressSession.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Matrícula inativa.");
    }

    @Test
    void register_enrollmentFromDifferentClassGroup() {
        Enrollment otherEnrollment = new Enrollment();
        otherEnrollment.setStudio(studio);
        otherEnrollment.setStudent(student);
        otherEnrollment.setClassGroup(otherClassGroup);
        otherEnrollment.setEnrollmentDate(LocalDate.now());
        otherEnrollment.setActive(true);
        otherEnrollment = enrollmentRepository.save(otherEnrollment);

        AttendanceRequest request = new AttendanceRequest(
                otherEnrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );

        assertThatThrownBy(() -> attendanceService.register(inProgressSession.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Matrícula não pertence à turma da sessão.");
    }

    @Test
    void register_duplicateAttendance_updatesExisting() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                "First registration"
        );
        var first = attendanceService.register(inProgressSession.getId(), request);

        AttendanceRequest updateRequest = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.ABSENT,
                "Updated notes"
        );
        var second = attendanceService.register(inProgressSession.getId(), updateRequest);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.status()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(second.notes()).isEqualTo("Updated notes");
    }

    @Test
    void register_updateExistingAttendance() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                "Initial notes"
        );
        var response = attendanceService.register(inProgressSession.getId(), request);

        AttendanceRequest updateRequest = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.JUSTIFIED,
                "Justified absence"
        );
        var updated = attendanceService.register(inProgressSession.getId(), updateRequest);

        assertThat(updated.id()).isEqualTo(response.id());
        assertThat(updated.status()).isEqualTo(AttendanceStatus.JUSTIFIED);
        assertThat(updated.notes()).isEqualTo("Justified absence");
    }

    @Test
    void findBySessionId_returnsAttendances() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );
        attendanceService.register(inProgressSession.getId(), request);

        var result = attendanceService.findBySessionId(inProgressSession.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).classSessionId()).isEqualTo(inProgressSession.getId());
    }

    @Test
    void findBySessionId_sessionNotFound() {
        assertThatThrownBy(() -> attendanceService.findBySessionId(java.util.UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Class session not found");
    }

    @Test
    void findBySessionId_emptyList() {
        var result = attendanceService.findBySessionId(completedSession.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findByEnrollmentId_returnsHistory() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );
        attendanceService.register(inProgressSession.getId(), request);

        var result = attendanceService.findByEnrollmentId(enrollment.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).enrollmentId()).isEqualTo(enrollment.getId());
    }

    @Test
    void findByEnrollmentId_enrollmentNotFound() {
        assertThatThrownBy(() -> attendanceService.findByEnrollmentId(java.util.UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Enrollment not found");
    }

    @Test
    void findByEnrollmentId_emptyList() {
        var result = attendanceService.findByEnrollmentId(enrollment.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void register_whenSessionCompleted_throwsConflictException() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );

        assertThatThrownBy(() -> attendanceService.register(completedSession.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A presença não pode ser registrada após a conclusão da aula.");
    }

    @Test
    void register_updateWhenSessionCompleted_throwsConflictException() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                "Initial"
        );
        attendanceService.register(inProgressSession.getId(), request);

        ClassSession session = classSessionRepository.findById(inProgressSession.getId()).orElseThrow();
        session.setStatus(ClassSessionStatus.COMPLETED);
        classSessionRepository.save(session);

        AttendanceRequest updateRequest = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.ABSENT,
                "Updated"
        );

        assertThatThrownBy(() -> attendanceService.register(inProgressSession.getId(), updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A presença não pode ser registrada após a conclusão da aula.");
    }

    @Test
    void findByClassGroupAndDate_returnsAttendances() {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                null
        );
        attendanceService.register(inProgressSession.getId(), request);

        var result = attendanceService.findByClassGroupAndDate(classGroup.getId(), LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).classSessionId()).isEqualTo(inProgressSession.getId());
        assertThat(result.get(0).enrollmentId()).isEqualTo(enrollment.getId());
        assertThat(result.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void findByClassGroupAndDate_classGroupNotFound() {
        assertThatThrownBy(() -> attendanceService.findByClassGroupAndDate(java.util.UUID.randomUUID(), LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Class group not found");
    }

    @Test
    void findByClassGroupAndDate_emptyList() {
        var result = attendanceService.findByClassGroupAndDate(classGroup.getId(), LocalDate.now());
        assertThat(result).isEmpty();
    }

    @Test
    void bulkSave_validEntries() {
        var item = new AttendanceBulkItem();
        item.setSessionId(inProgressSession.getId());
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);
        item.setNotes("Present");

        var response = attendanceService.bulkSave(studio.getId(), List.of(item));

        assertThat(response.savedCount()).isEqualTo(1);
    }

    @Test
    void bulkSave_multipleEntries() {
        var item1 = new AttendanceBulkItem();
        item1.setSessionId(inProgressSession.getId());
        item1.setEnrollmentId(enrollment.getId());
        item1.setStatus(AttendanceStatus.PRESENT);
        item1.setNotes("First");

        var item2 = new AttendanceBulkItem();
        item2.setSessionId(inProgressSession.getId());
        item2.setEnrollmentId(enrollment.getId());
        item2.setStatus(AttendanceStatus.ABSENT);
        item2.setNotes("Second");

        var response = attendanceService.bulkSave(studio.getId(), List.of(item1, item2));

        assertThat(response.savedCount()).isEqualTo(2);
    }

    @Test
    void bulkSave_upsertExisting() {
        var item1 = new AttendanceBulkItem();
        item1.setSessionId(inProgressSession.getId());
        item1.setEnrollmentId(enrollment.getId());
        item1.setStatus(AttendanceStatus.PRESENT);
        item1.setNotes("Original");
        attendanceService.bulkSave(studio.getId(), List.of(item1));

        var item2 = new AttendanceBulkItem();
        item2.setSessionId(inProgressSession.getId());
        item2.setEnrollmentId(enrollment.getId());
        item2.setStatus(AttendanceStatus.JUSTIFIED);
        item2.setNotes("Updated");
        var response = attendanceService.bulkSave(studio.getId(), List.of(item2));

        assertThat(response.savedCount()).isEqualTo(1);

        var attendances = attendanceRepository.findByClassSessionId(inProgressSession.getId());
        assertThat(attendances).hasSize(1);
        assertThat(attendances.get(0).getStatus()).isEqualTo(AttendanceStatus.JUSTIFIED);
        assertThat(attendances.get(0).getNotes()).isEqualTo("Updated");
    }

    @Test
    void bulkSave_usingStudentId() {
        var item = new AttendanceBulkItem();
        item.setSessionId(inProgressSession.getId());
        item.setStudentId(student.getId());
        item.setPresent(true);
        item.setObservation("Via studentId");

        var response = attendanceService.bulkSave(studio.getId(), List.of(item));

        assertThat(response.savedCount()).isEqualTo(1);

        var attendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
        assertThat(attendances).hasSize(1);
        assertThat(attendances.get(0).getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(attendances.get(0).getNotes()).isEqualTo("Via studentId");
    }

    @Test
    void bulkSave_usingPresentBoolean() {
        var item = new AttendanceBulkItem();
        item.setSessionId(inProgressSession.getId());
        item.setEnrollmentId(enrollment.getId());
        item.setPresent(false);

        var response = attendanceService.bulkSave(studio.getId(), List.of(item));

        assertThat(response.savedCount()).isEqualTo(1);

        var attendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
        assertThat(attendances.get(0).getStatus()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    void bulkSave_enrollmentFromDifferentStudio_throwsConflictException() {
        var otherStudio = new Studio();
        otherStudio.setName("Other Studio");
        var savedOtherStudio = studioRepository.save(otherStudio);

        var item = new AttendanceBulkItem();
        item.setSessionId(inProgressSession.getId());
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);

        assertThatThrownBy(() -> attendanceService.bulkSave(savedOtherStudio.getId(), List.of(item)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("studio");
    }

    @Test
    void bulkSave_sessionNotFound_throwsResourceNotFoundException() {
        var item = new AttendanceBulkItem();
        item.setSessionId(java.util.UUID.randomUUID());
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);

        assertThatThrownBy(() -> attendanceService.bulkSave(studio.getId(), List.of(item)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sessão não encontrada");
    }

    @Test
    void bulkSave_enrollmentNotFound_throwsResourceNotFoundException() {
        var item = new AttendanceBulkItem();
        item.setSessionId(inProgressSession.getId());
        item.setEnrollmentId(java.util.UUID.randomUUID());
        item.setStatus(AttendanceStatus.PRESENT);

        assertThatThrownBy(() -> attendanceService.bulkSave(studio.getId(), List.of(item)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Matrícula não encontrada");
    }
}
