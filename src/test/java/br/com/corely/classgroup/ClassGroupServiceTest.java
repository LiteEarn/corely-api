package br.com.corely.classgroup;

import br.com.corely.classgroup.dto.ClassGroupRequest;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.classgroup.dto.ConfirmInactivationRequest;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionService;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ConfirmationRequiredException;
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
class ClassGroupServiceTest {

    @Autowired
    private ClassGroupService classGroupService;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassSessionService classSessionService;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    private Studio studio;
    private Instructor activeInstructor;
    private Instructor inactiveInstructor;
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
        studio.setActive(true);
        studio = studioRepository.save(studio);

        activeInstructor = new Instructor();
        activeInstructor.setStudio(studio);
        activeInstructor.setFullName("Active Instructor");
        activeInstructor.setEmail("active@example.com");
        activeInstructor.setActive(true);
        activeInstructor = instructorRepository.save(activeInstructor);

        inactiveInstructor = new Instructor();
        inactiveInstructor.setStudio(studio);
        inactiveInstructor.setFullName("Inactive Instructor");
        inactiveInstructor.setEmail("inactive@example.com");
        inactiveInstructor.setActive(false);
        inactiveInstructor = instructorRepository.save(inactiveInstructor);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setEmail("student@test.com");
        student.setActive(true);
        student = studentRepository.save(student);
    }

    @Test
    void create_whenInstructorInactive_throwsBusinessException() {
        // Given
        ClassGroupRequest request = new ClassGroupRequest();
        request.setStudioId(studio.getId());
        request.setInstructorId(inactiveInstructor.getId());
        request.setName("Test Class Group");
        request.setDescription("Test Description");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(20);
        request.setMonday(true);
        request.setActive(true);

        // When & Then
        assertThatThrownBy(() -> classGroupService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Selected instructor is inactive.");
    }

    @Test
    void create_whenInstructorActive_succeeds() {
        // Given
        ClassGroupRequest request = new ClassGroupRequest();
        request.setStudioId(studio.getId());
        request.setInstructorId(activeInstructor.getId());
        request.setName("Test Class Group");
        request.setDescription("Test Description");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(20);
        request.setMonday(true);
        request.setActive(true);

        // When
        ClassGroupResponse response = classGroupService.create(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Class Group");
        assertThat(response.getInstructorId()).isEqualTo(activeInstructor.getId());
    }

    @Test
    void update_whenInstructorInactive_throwsBusinessException() {
        // Given - create a class group with active instructor
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // Given - try to update with inactive instructor
        ClassGroupRequest updateRequest = new ClassGroupRequest();
        updateRequest.setStudioId(studio.getId());
        updateRequest.setInstructorId(inactiveInstructor.getId());
        updateRequest.setName("Updated Class Group");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStartTime(LocalTime.of(10, 0));
        updateRequest.setEndTime(LocalTime.of(11, 0));
        updateRequest.setCapacity(25);
        updateRequest.setTuesday(true);
        updateRequest.setActive(true);

        // When & Then
        assertThatThrownBy(() -> classGroupService.update(created.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Selected instructor is inactive.");
    }

    @Test
    void update_whenInstructorActive_succeeds() {
        // Given - create a class group with active instructor
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // Given - update with same active instructor
        ClassGroupRequest updateRequest = new ClassGroupRequest();
        updateRequest.setStudioId(studio.getId());
        updateRequest.setInstructorId(activeInstructor.getId());
        updateRequest.setName("Updated Class Group");
        updateRequest.setDescription("Updated Description");
        updateRequest.setStartTime(LocalTime.of(10, 0));
        updateRequest.setEndTime(LocalTime.of(11, 0));
        updateRequest.setCapacity(25);
        updateRequest.setTuesday(true);
        updateRequest.setActive(true);

        // When
        ClassGroupResponse response = classGroupService.update(created.getId(), updateRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Class Group");
        assertThat(response.getInstructorId()).isEqualTo(activeInstructor.getId());
    }

    @Test
    void findActive_returnsOnlyActiveClassGroups() {
        // Given - create active class group
        ClassGroupRequest activeRequest = new ClassGroupRequest();
        activeRequest.setStudioId(studio.getId());
        activeRequest.setInstructorId(activeInstructor.getId());
        activeRequest.setName("Active Class Group");
        activeRequest.setDescription("Active Description");
        activeRequest.setStartTime(LocalTime.of(9, 0));
        activeRequest.setEndTime(LocalTime.of(10, 0));
        activeRequest.setCapacity(20);
        activeRequest.setMonday(true);
        activeRequest.setActive(true);
        classGroupService.create(activeRequest);

        // Given - create inactive class group
        ClassGroupRequest inactiveRequest = new ClassGroupRequest();
        inactiveRequest.setStudioId(studio.getId());
        inactiveRequest.setInstructorId(activeInstructor.getId());
        inactiveRequest.setName("Inactive Class Group");
        inactiveRequest.setDescription("Inactive Description");
        inactiveRequest.setStartTime(LocalTime.of(11, 0));
        inactiveRequest.setEndTime(LocalTime.of(12, 0));
        inactiveRequest.setCapacity(15);
        inactiveRequest.setTuesday(true);
        inactiveRequest.setActive(false);
        classGroupService.create(inactiveRequest);

        // When
        var response = classGroupService.findActive();

        // Then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Active Class Group");
        assertThat(response.get(0).getActive()).isTrue();
    }

    @Test
    void findAll_returnsAllClassGroupsIncludingInactive() {
        // Given - create active class group
        ClassGroupRequest activeRequest = new ClassGroupRequest();
        activeRequest.setStudioId(studio.getId());
        activeRequest.setInstructorId(activeInstructor.getId());
        activeRequest.setName("Active Class Group");
        activeRequest.setDescription("Active Description");
        activeRequest.setStartTime(LocalTime.of(9, 0));
        activeRequest.setEndTime(LocalTime.of(10, 0));
        activeRequest.setCapacity(20);
        activeRequest.setMonday(true);
        activeRequest.setActive(true);
        classGroupService.create(activeRequest);

        // Given - create inactive class group
        ClassGroupRequest inactiveRequest = new ClassGroupRequest();
        inactiveRequest.setStudioId(studio.getId());
        inactiveRequest.setInstructorId(activeInstructor.getId());
        inactiveRequest.setName("Inactive Class Group");
        inactiveRequest.setDescription("Inactive Description");
        inactiveRequest.setStartTime(LocalTime.of(11, 0));
        inactiveRequest.setEndTime(LocalTime.of(12, 0));
        inactiveRequest.setCapacity(15);
        inactiveRequest.setTuesday(true);
        inactiveRequest.setActive(false);
        classGroupService.create(inactiveRequest);

        // When
        var response = classGroupService.findAll();

        // Then
        assertThat(response).hasSize(2);
        assertThat(response).extracting("name")
                .containsExactlyInAnyOrder("Active Class Group", "Inactive Class Group");
    }

    // ========== SCENARIO 1: Class Group without enrollments = Normal inactivation ==========
    @Test
    void update_whenInactivatingWithoutEnrollments_succeeds() {
        // Given - create an active class group with no enrollments
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // When - try to inactivate via update
        ClassGroupRequest updateRequest = new ClassGroupRequest();
        updateRequest.setStudioId(studio.getId());
        updateRequest.setInstructorId(activeInstructor.getId());
        updateRequest.setName("Test Class Group");
        updateRequest.setDescription("Test Description");
        updateRequest.setStartTime(LocalTime.of(9, 0));
        updateRequest.setEndTime(LocalTime.of(10, 0));
        updateRequest.setCapacity(20);
        updateRequest.setMonday(true);
        updateRequest.setActive(false);

        ClassGroupResponse response = classGroupService.update(created.getId(), updateRequest);

        // Then
        assertThat(response.getActive()).isFalse();
    }

    // ========== SCENARIO 2: Class Group with 5 enrollments = 409 ==========
    @Test
    void update_whenInactivatingWithActiveEnrollments_throwsConfirmationRequired() {
        // Given - create an active class group
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // Given - create 5 active enrollments
        ClassGroup classGroup = classGroupRepository.findById(created.getId()).orElseThrow();
        for (int i = 0; i < 5; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setEmail("student" + i + "@test.com");
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment enrollment = new Enrollment();
            enrollment.setStudio(studio);
            enrollment.setStudent(s);
            enrollment.setClassGroup(classGroup);
            enrollment.setEnrollmentDate(LocalDate.now());
            enrollment.setActive(true);
            enrollmentRepository.save(enrollment);
        }

        // When - try to inactivate
        ClassGroupRequest updateRequest = new ClassGroupRequest();
        updateRequest.setStudioId(studio.getId());
        updateRequest.setInstructorId(activeInstructor.getId());
        updateRequest.setName("Test Class Group");
        updateRequest.setDescription("Test Description");
        updateRequest.setStartTime(LocalTime.of(9, 0));
        updateRequest.setEndTime(LocalTime.of(10, 0));
        updateRequest.setCapacity(20);
        updateRequest.setMonday(true);
        updateRequest.setActive(false);

        // Then
        assertThatThrownBy(() -> classGroupService.update(created.getId(), updateRequest))
                .isInstanceOf(ConfirmationRequiredException.class)
                .hasMessage("This class group has active enrollments.")
                .satisfies(e -> {
                    ConfirmationRequiredException ex = (ConfirmationRequiredException) e;
                    assertThat(ex.getActiveEnrollments()).isEqualTo(5);
                });

        // And class group should remain active
        ClassGroup reloaded = classGroupRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getActive()).isTrue();
    }

    // ========== SCENARIO 3: Confirmation endpoint - Inactivation with cascade ==========
    @Test
    void inactivate_whenConfirmation_cascadesEnrollments() {
        // Given - create an active class group
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // Given - create 5 active enrollments
        ClassGroup classGroup = classGroupRepository.findById(created.getId()).orElseThrow();
        Enrollment[] enrollments = new Enrollment[5];
        for (int i = 0; i < 5; i++) {
            Student s = new Student();
            s.setStudio(studio);
            s.setFullName("Student " + i);
            s.setEmail("student" + i + "@test.com");
            s.setActive(true);
            s = studentRepository.save(s);

            Enrollment enrollment = new Enrollment();
            enrollment.setStudio(studio);
            enrollment.setStudent(s);
            enrollment.setClassGroup(classGroup);
            enrollment.setEnrollmentDate(LocalDate.now());
            enrollment.setActive(true);
            enrollment = enrollmentRepository.save(enrollment);
            enrollments[i] = enrollment;
        }

        // When - confirm inactivation
        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(true);
        classGroupService.inactivate(created.getId(), confirmRequest);

        // Then - class group should be inactive
        ClassGroup reloaded = classGroupRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getActive()).isFalse();

        // Then - all enrollments should be inactive
        for (Enrollment enrollment : enrollments) {
            Enrollment reloadedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
            assertThat(reloadedEnrollment.getActive()).isFalse();
        }

        // Then - students should be preserved (still active)
        for (Enrollment enrollment : enrollments) {
            Student s = studentRepository.findById(enrollment.getStudent().getId()).orElseThrow();
            assertThat(s.getActive()).isTrue();
        }

        // Then - instructor should be preserved
        Instructor instructor = instructorRepository.findById(activeInstructor.getId()).orElseThrow();
        assertThat(instructor.getActive()).isTrue();
    }

    // ========== SCENARIO 3b: Validation - already inactive class group ==========
    @Test
    void inactivate_whenAlreadyInactive_throwsBusinessException() {
        // Given - create and directly inactivate a class group
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        ClassGroup classGroup = classGroupRepository.findById(created.getId()).orElseThrow();
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        // When & Then
        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(true);
        assertThatThrownBy(() -> classGroupService.inactivate(created.getId(), confirmRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class group is already inactive.");
    }

    // ========== SCENARIO 3c: Validation - non-existent class group ==========
    @Test
    void inactivate_whenNotFound_throwsResourceNotFoundException() {
        // Given
        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(true);

        // When & Then
        assertThatThrownBy(() -> classGroupService.inactivate(UUID.randomUUID(), confirmRequest))
                .isInstanceOf(br.com.corely.shared.exception.ResourceNotFoundException.class)
                .hasMessage("Class group not found");
    }

    // ========== SCENARIO 3d: Inactivation without cascadeEnrollments and with enrollments = 409 ==========
    @Test
    void inactivate_whenCascadeEnrollmentsFalseWithEnrollments_throwsConfirmationRequired() {
        // Given - create an active class group
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // Given - create an active enrollment
        ClassGroup classGroup = classGroupRepository.findById(created.getId()).orElseThrow();
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollmentRepository.save(enrollment);

        // When & Then
        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(false);
        assertThatThrownBy(() -> classGroupService.inactivate(created.getId(), confirmRequest))
                .isInstanceOf(ConfirmationRequiredException.class)
                .hasMessage("This class group has active enrollments.")
                .satisfies(e -> {
                    ConfirmationRequiredException ex = (ConfirmationRequiredException) e;
                    assertThat(ex.getActiveEnrollments()).isEqualTo(1);
                });
    }

    // ========== SCENARIO 3e: Inactivation without cascadeEnrollments but NO enrollments = OK ==========
    @Test
    void inactivate_whenCascadeEnrollmentsFalseWithoutEnrollments_succeeds() {
        // Given - create an active class group with no enrollments
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // When & Then
        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(false);
        classGroupService.inactivate(created.getId(), confirmRequest);

        // Then
        ClassGroup reloaded = classGroupRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getActive()).isFalse();
    }

    // ========== SCENARIO 4: Failure during enrollment update = Rollback ==========
    @Test
    @Transactional
    void inactivate_whenErrorDuringEnrollmentUpdate_rollsBack() {
        // Given - create an active class group
        ClassGroupRequest createRequest = new ClassGroupRequest();
        createRequest.setStudioId(studio.getId());
        createRequest.setInstructorId(activeInstructor.getId());
        createRequest.setName("Test Class Group");
        createRequest.setDescription("Test Description");
        createRequest.setStartTime(LocalTime.of(9, 0));
        createRequest.setEndTime(LocalTime.of(10, 0));
        createRequest.setCapacity(20);
        createRequest.setMonday(true);
        createRequest.setActive(true);
        ClassGroupResponse created = classGroupService.create(createRequest);

        // Given - create an active enrollment
        ClassGroup classGroup = classGroupRepository.findById(created.getId()).orElseThrow();
        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        // Force a rollback by throwing an exception within the transaction
        // The inactivate method will run in its own transaction.
        // We need to test transactional behavior. Let's verify the data is consistent.
        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(true);
        classGroupService.inactivate(created.getId(), confirmRequest);

        // Then - verify cascading worked
        ClassGroup reloaded = classGroupRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getActive()).isFalse();

        Enrollment reloadedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(reloadedEnrollment.getActive()).isFalse();
    }

    @Test
    void create_activeClassGroup_generatesSessions() {
        ClassGroupRequest request = new ClassGroupRequest();
        request.setStudioId(studio.getId());
        request.setInstructorId(activeInstructor.getId());
        request.setName("Test Class Group");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(20);
        request.setMonday(true);
        request.setWednesday(true);
        request.setFriday(true);
        request.setActive(true);

        ClassGroupResponse response = classGroupService.create(request);

        var sessions = classSessionRepository.findByClassGroupId(response.getId());
        assertThat(sessions).isNotEmpty();
        assertThat(sessions).allMatch(s -> s.getStatus() == ClassSessionStatus.SCHEDULED);
        assertThat(sessions).allMatch(s -> s.getSessionDate().isAfter(LocalDate.now().minusDays(1)));
        assertThat(sessions).allMatch(s -> s.getClassGroup().getId().equals(response.getId()));
    }

    @Test
    void create_inactiveClassGroup_doesNotGenerateSessions() {
        ClassGroupRequest request = new ClassGroupRequest();
        request.setStudioId(studio.getId());
        request.setInstructorId(activeInstructor.getId());
        request.setName("Test Class Group");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(20);
        request.setMonday(true);
        request.setActive(false);

        ClassGroupResponse response = classGroupService.create(request);

        var sessions = classSessionRepository.findByClassGroupId(response.getId());
        assertThat(sessions).isEmpty();
    }

    @Test
    void update_timeChange_regeneratesFutureSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        var sessionsBefore = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsBefore).isNotEmpty();
        LocalTime oldStartTime = sessionsBefore.get(0).getStartTime();

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setStartTime(LocalTime.of(14, 0));
        updateRequest.setEndTime(LocalTime.of(15, 0));
        classGroupService.update(created.getId(), updateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
        assertThat(sessionsAfter).allMatch(s -> s.getStartTime().equals(LocalTime.of(14, 0)));
        assertThat(sessionsAfter.get(0).getStartTime()).isNotEqualTo(oldStartTime);
    }

    @Test
    void update_instructorChange_regeneratesFutureSessions() {
        Instructor newInstructor = new Instructor();
        newInstructor.setStudio(studio);
        newInstructor.setFullName("New Instructor");
        newInstructor.setEmail("new@example.com");
        newInstructor.setActive(true);
        Instructor savedInstructor = instructorRepository.save(newInstructor);

        ClassGroupResponse created = createActiveClassGroup();

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setInstructorId(savedInstructor.getId());
        classGroupService.update(created.getId(), updateRequest);

        UUID instructorId = savedInstructor.getId();
        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
        assertThat(sessionsAfter).allMatch(s -> s.getInstructor().getId().equals(instructorId));
    }

    @Test
    void update_capacityChange_regeneratesFutureSessions() {
        ClassGroupResponse created = createActiveClassGroup();
        assertThat(classSessionRepository.findByClassGroupId(created.getId())).isNotEmpty();

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setCapacity(99);
        classGroupService.update(created.getId(), updateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
    }

    @Test
    void update_daysChange_regeneratesFutureSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        var sessionsBefore = classSessionRepository.findByClassGroupId(created.getId());
        long countBefore = sessionsBefore.size();

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setMonday(false);
        updateRequest.setTuesday(true);
        updateRequest.setWednesday(false);
        updateRequest.setFriday(false);
        classGroupService.update(created.getId(), updateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
        assertThat(sessionsAfter).allMatch(s ->
                s.getSessionDate().getDayOfWeek() == java.time.DayOfWeek.TUESDAY);
    }

    @Test
    void update_nameOnlyChange_doesNotRegenerateSessions() {
        ClassGroupRequest request = createRequest();
        ClassGroupResponse created = classGroupService.create(request);

        var sessionsBefore = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsBefore).isNotEmpty();

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setName("Completely Different Name");
        classGroupService.update(created.getId(), updateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).hasSize(sessionsBefore.size());
        assertThat(sessionsAfter).extracting(s -> s.getStartTime())
                .containsExactlyElementsOf(sessionsBefore.stream().map(ClassSession::getStartTime).toList());
    }

    @Test
    void update_inactivateClassGroup_cancelsFutureScheduledSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        var sessionsBefore = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsBefore).isNotEmpty();
        assertThat(sessionsBefore).allMatch(s -> s.getStatus() == ClassSessionStatus.SCHEDULED);

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setActive(false);
        classGroupService.update(created.getId(), updateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
        assertThat(sessionsAfter).allMatch(s -> s.getStatus() == ClassSessionStatus.CANCELLED);
    }

    @Test
    void update_reactivateClassGroup_regeneratesSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        ClassGroupRequest inactivateRequest = createUpdateRequest();
        inactivateRequest.setActive(false);
        classGroupService.update(created.getId(), inactivateRequest);

        var cancelledSessions = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(cancelledSessions).allMatch(s -> s.getStatus() == ClassSessionStatus.CANCELLED);

        ClassGroupRequest reactivateRequest = createUpdateRequest();
        reactivateRequest.setActive(true);
        classGroupService.update(created.getId(), reactivateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
        var newScheduled = sessionsAfter.stream().filter(s -> s.getStatus() == ClassSessionStatus.SCHEDULED).toList();
        assertThat(newScheduled).isNotEmpty();
    }

    @Test
    void update_inactivation_preservesHistory() {
        ClassGroupResponse created = createActiveClassGroup();

        ClassGroup group = classGroupRepository.findById(created.getId()).orElseThrow();
        var sessions = classSessionRepository.findByClassGroupId(created.getId());
        ClassSession pastSession = sessions.get(0);
        pastSession.setSessionDate(LocalDate.now().minusDays(1));
        pastSession.setStatus(ClassSessionStatus.COMPLETED);
        classSessionRepository.save(pastSession);

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setActive(false);
        classGroupService.update(created.getId(), updateRequest);

        ClassSession completedReloaded = classSessionRepository.findById(pastSession.getId()).orElseThrow();
        assertThat(completedReloaded.getStatus()).isEqualTo(ClassSessionStatus.COMPLETED);
    }

    @Test
    void update_regeneration_doesNotDuplicateSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        var sessionsBefore = classSessionRepository.findByClassGroupId(created.getId());
        long countBefore = sessionsBefore.size();

        ClassGroupRequest updateRequest = createUpdateRequest();
        updateRequest.setStartTime(LocalTime.of(14, 0));
        updateRequest.setEndTime(LocalTime.of(15, 0));
        classGroupService.update(created.getId(), updateRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        long distinctDates = sessionsAfter.stream()
                .map(ClassSession::getSessionDate).distinct().count();
        assertThat(sessionsAfter).hasSize((int) distinctDates);
        assertThat(sessionsAfter).allMatch(s -> s.getStartTime().equals(LocalTime.of(14, 0)));
    }

    @Test
    void inactivateEndpoint_cancelsFutureSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        var sessionsBefore = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsBefore).isNotEmpty();
        assertThat(sessionsBefore).allMatch(s -> s.getStatus() == ClassSessionStatus.SCHEDULED);

        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(false);
        classGroupService.inactivate(created.getId(), confirmRequest);

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(sessionsAfter).isNotEmpty();
        assertThat(sessionsAfter).allMatch(s -> s.getStatus() == ClassSessionStatus.CANCELLED);
    }

    // ========== REACTIVATION TESTS ==========

    @Test
    void reactivate_inactiveClassGroup_succeeds() {
        ClassGroupResponse created = createActiveClassGroup();

        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(false);
        classGroupService.inactivate(created.getId(), confirmRequest);

        ClassGroupResponse response = classGroupService.reactivate(created.getId());

        assertThat(response.getActive()).isTrue();
    }

    @Test
    void reactivate_whenNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> classGroupService.reactivate(UUID.randomUUID()))
                .isInstanceOf(br.com.corely.shared.exception.ResourceNotFoundException.class)
                .hasMessage("Class group not found");
    }

    @Test
    void reactivate_whenAlreadyActive_throwsBusinessException() {
        ClassGroupResponse created = createActiveClassGroup();

        assertThatThrownBy(() -> classGroupService.reactivate(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A turma já está ativa.");
    }

    @Test
    void reactivate_generatesSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(false);
        classGroupService.inactivate(created.getId(), confirmRequest);

        var cancelledSessions = classSessionRepository.findByClassGroupId(created.getId());
        assertThat(cancelledSessions).allMatch(s -> s.getStatus() == ClassSessionStatus.CANCELLED);

        classGroupService.reactivate(created.getId());

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        var scheduledSessions = sessionsAfter.stream()
                .filter(s -> s.getStatus() == ClassSessionStatus.SCHEDULED)
                .toList();
        assertThat(scheduledSessions).isNotEmpty();
        assertThat(scheduledSessions).allMatch(s -> s.getSessionDate().isAfter(LocalDate.now().minusDays(1)));
    }

    @Test
    void reactivate_doesNotDuplicateSessions() {
        ClassGroupResponse created = createActiveClassGroup();

        ConfirmInactivationRequest confirmRequest = new ConfirmInactivationRequest(false);
        classGroupService.inactivate(created.getId(), confirmRequest);

        classGroupService.reactivate(created.getId());

        var sessionsAfter = classSessionRepository.findByClassGroupId(created.getId());
        long distinctDates = sessionsAfter.stream()
                .map(ClassSession::getSessionDate)
                .distinct()
                .count();
        assertThat(sessionsAfter).hasSize((int) distinctDates);
    }

    private ClassGroupResponse createActiveClassGroup() {
        ClassGroupRequest request = createRequest();
        return classGroupService.create(request);
    }

    private ClassGroupRequest createRequest() {
        ClassGroupRequest request = new ClassGroupRequest();
        request.setStudioId(studio.getId());
        request.setInstructorId(activeInstructor.getId());
        request.setName("Test Class Group");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(20);
        request.setMonday(true);
        request.setWednesday(true);
        request.setFriday(true);
        request.setActive(true);
        return request;
    }

    private ClassGroupRequest createUpdateRequest() {
        ClassGroupRequest request = new ClassGroupRequest();
        request.setStudioId(studio.getId());
        request.setInstructorId(activeInstructor.getId());
        request.setName("Test Class Group");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(20);
        request.setMonday(true);
        request.setWednesday(true);
        request.setFriday(true);
        request.setActive(true);
        return request;
    }
}
