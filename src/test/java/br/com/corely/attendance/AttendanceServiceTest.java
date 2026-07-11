package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.BulkAttendanceRequest;
import br.com.corely.attendance.dto.SessionAttendanceResponse;
import br.com.corely.attendance.dto.SessionBulkAttendanceRequest;
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
    void saveSessionAttendances_createNew() {
        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item = new SessionBulkAttendanceRequest.AttendanceItem();
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);
        request.setAttendances(List.of(item));

        var responses = attendanceService.saveSessionAttendances(inProgressSession.getId(), request);

        assertThat(responses).hasSize(1);
        SessionAttendanceResponse response = responses.get(0);
        assertThat(response.classSessionId()).isEqualTo(inProgressSession.getId());
        assertThat(response.enrollmentId()).isEqualTo(enrollment.getId());
        assertThat(response.studentId()).isEqualTo(student.getId());
        assertThat(response.studentName()).isEqualTo(student.getFullName());
        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void saveSessionAttendances_updateExisting() {
        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item = new SessionBulkAttendanceRequest.AttendanceItem();
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);
        request.setAttendances(List.of(item));
        var first = attendanceService.saveSessionAttendances(inProgressSession.getId(), request);

        item.setStatus(AttendanceStatus.ABSENT);
        var second = attendanceService.saveSessionAttendances(inProgressSession.getId(), request);

        assertThat(second).hasSize(1);
        assertThat(second.get(0).id()).isEqualTo(first.get(0).id());
        assertThat(second.get(0).status()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    void saveSessionAttendances_preventsDuplicate() {
        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item = new SessionBulkAttendanceRequest.AttendanceItem();
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.JUSTIFIED);
        request.setAttendances(List.of(item));
        attendanceService.saveSessionAttendances(inProgressSession.getId(), request);

        item.setStatus(AttendanceStatus.PRESENT);
        var result = attendanceService.saveSessionAttendances(inProgressSession.getId(), request);

        assertThat(result).hasSize(1);
        var allForSession = attendanceService.findBySessionId(inProgressSession.getId());
        assertThat(allForSession).hasSize(1);
    }

    @Test
    void saveSessionAttendances_multipleStudents() {
        Student student2 = new Student();
        student2.setStudio(studio);
        student2.setFullName("Second Student");
        student2.setEmail("second@test.com");
        student2.setActive(true);
        student2 = studentRepository.save(student2);

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudio(studio);
        enrollment2.setStudent(student2);
        enrollment2.setClassGroup(classGroup);
        enrollment2.setEnrollmentDate(LocalDate.now());
        enrollment2.setActive(true);
        enrollment2 = enrollmentRepository.save(enrollment2);

        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item1 = new SessionBulkAttendanceRequest.AttendanceItem();
        item1.setEnrollmentId(enrollment.getId());
        item1.setStatus(AttendanceStatus.PRESENT);
        var item2 = new SessionBulkAttendanceRequest.AttendanceItem();
        item2.setEnrollmentId(enrollment2.getId());
        item2.setStatus(AttendanceStatus.ABSENT);
        request.setAttendances(List.of(item1, item2));

        var responses = attendanceService.saveSessionAttendances(inProgressSession.getId(), request);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(responses.get(1).status()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    void saveSessionAttendances_inactiveEnrollment_throwsConflictException() {
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item = new SessionBulkAttendanceRequest.AttendanceItem();
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);
        request.setAttendances(List.of(item));

        assertThatThrownBy(() -> attendanceService.saveSessionAttendances(inProgressSession.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Matrícula inativa.");
    }

    @Test
    void saveSessionAttendances_completedSession_throwsConflictException() {
        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item = new SessionBulkAttendanceRequest.AttendanceItem();
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);
        request.setAttendances(List.of(item));

        assertThatThrownBy(() -> attendanceService.saveSessionAttendances(completedSession.getId(), request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A presença não pode ser registrada após a conclusão da aula.");
    }

    private BulkAttendanceRequest.AttendanceItem attendanceItem(UUID studentId, boolean present, String observation) {
        var item = new BulkAttendanceRequest.AttendanceItem();
        item.setStudentId(studentId);
        item.setPresent(present);
        item.setObservation(observation);
        return item;
    }

    private BulkAttendanceRequest bulkRequest(List<BulkAttendanceRequest.AttendanceItem> items) {
        var req = new BulkAttendanceRequest();
        req.setStudioId(studio.getId());
        req.setClassGroupId(classGroup.getId());
        req.setAttendanceDate(LocalDate.now());
        req.setAttendances(items);
        return req;
    }

    @Test
    void bulkSave_validEntries() {
        var response = attendanceService.bulkSave(
                bulkRequest(List.of(attendanceItem(student.getId(), true, "Present"))));

        assertThat(response.savedCount()).isEqualTo(1);
    }

    @Test
    void bulkSave_multipleEntries() {
        var response = attendanceService.bulkSave(bulkRequest(List.of(
                attendanceItem(student.getId(), true, "First"),
                attendanceItem(student.getId(), false, "Second"))));

        assertThat(response.savedCount()).isEqualTo(2);
    }

    @Test
    void bulkSave_upsertExisting() {
        var item = attendanceItem(student.getId(), true, "Original");
        attendanceService.bulkSave(bulkRequest(List.of(item)));

        var updated = attendanceItem(student.getId(), false, "Updated");
        var response = attendanceService.bulkSave(bulkRequest(List.of(updated)));

        assertThat(response.savedCount()).isEqualTo(1);

        var attendances = attendanceRepository.findByClassSessionId(inProgressSession.getId());
        assertThat(attendances).hasSize(1);
        assertThat(attendances.get(0).getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(attendances.get(0).getNotes()).isEqualTo("Updated");
    }

    @Test
    void bulkSave_presentTrue_mapsToPRESENT() {
        attendanceService.bulkSave(
                bulkRequest(List.of(attendanceItem(student.getId(), true, null))));

        var attendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
        assertThat(attendances.get(0).getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void bulkSave_presentFalse_mapsToABSENT() {
        attendanceService.bulkSave(
                bulkRequest(List.of(attendanceItem(student.getId(), false, null))));

        var attendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
        assertThat(attendances.get(0).getStatus()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    void bulkSave_enrollmentFromDifferentStudio_throwsConflictException() {
        var otherStudio = new Studio();
        otherStudio.setName("Other Studio");
        var savedOtherStudio = studioRepository.save(otherStudio);

        var req = bulkRequest(List.of(attendanceItem(student.getId(), true, null)));
        req.setStudioId(savedOtherStudio.getId());

        assertThatThrownBy(() -> attendanceService.bulkSave(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("studio");
    }

    @Test
    void bulkSave_noInProgressSession_throwsConflictException() {
        inProgressSession.setStatus(ClassSessionStatus.SCHEDULED);
        classSessionRepository.save(inProgressSession);

        assertThatThrownBy(() -> attendanceService.bulkSave(
                bulkRequest(List.of(attendanceItem(student.getId(), true, null)))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("sessão em andamento");
    }
}
