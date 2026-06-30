package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
import br.com.corely.classsession.dto.SessionGenerationResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
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
class ClassSessionServiceTest {

    @Autowired
    private ClassSessionService classSessionService;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;

    @BeforeEach
    void setUp() {
        classSessionRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
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
    }

    @Test
    void create_whenValidRequest_succeeds() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());

        ClassSessionResponse response = classSessionService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getClassGroupId()).isEqualTo(classGroup.getId());
        assertThat(response.getInstructorId()).isEqualTo(instructor.getId());
        assertThat(response.getSessionDate()).isEqualTo(LocalDate.now());
        assertThat(response.getStartTime()).isEqualTo(classGroup.getStartTime());
        assertThat(response.getEndTime()).isEqualTo(classGroup.getEndTime());
        assertThat(response.getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
    }

    @Test
    void create_whenClassGroupNotFound_throwsResourceNotFoundException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(UUID.randomUUID());
        request.setSessionDate(LocalDate.now());

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Turma inexistente");
    }

    @Test
    void create_whenClassGroupInactive_throwsConflictException() {
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Turma inativa");
    }

    @Test
    void create_whenInstructorInactive_throwsConflictException() {
        instructor.setActive(false);
        instructorRepository.save(instructor);

        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Instrutor inativo");
    }

    @Test
    void create_whenDuplicateSession_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        classSessionService.create(request);

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe uma sessão para esta turma nesta data");
    }

    @Test
    void create_whenSessionDateInPast_throwsBusinessException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Data da sessão não pode ser anterior à data atual");
    }

    @Test
    void create_autoFillsInstructorAndTimeFromClassGroup() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());

        ClassSessionResponse response = classSessionService.create(request);

        assertThat(response.getInstructorId()).isEqualTo(instructor.getId());
        assertThat(response.getInstructorName()).isEqualTo("Test Instructor");
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
    }

    @Test
    void cancel_whenSessionExists_succeeds() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.cancel(created.getId());

        ClassSession cancelled = classSessionRepository.findById(created.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ClassSessionStatus.CANCELLED);
    }

    @Test
    void cancel_whenSessionNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> classSessionService.cancel(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sessão inexistente");
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.cancel(created.getId());

        assertThatThrownBy(() -> classSessionService.cancel(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A sessão já está cancelada.");
    }

    @Test
    void cancel_whenSessionCompleted_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());
        classSessionService.complete(created.getId());

        assertThatThrownBy(() -> classSessionService.cancel(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A sessão já foi concluída.");
    }

    @Test
    void findById_whenSessionExists_returnsSession() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        ClassSessionResponse found = classSessionService.findById(created.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getClassGroupId()).isEqualTo(classGroup.getId());
        assertThat(found.getInstructorId()).isEqualTo(instructor.getId());
        assertThat(found.getSessionDate()).isEqualTo(LocalDate.now());
        assertThat(found.getStatus()).isEqualTo(ClassSessionStatus.SCHEDULED);
    }

    @Test
    void findById_whenSessionNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> classSessionService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sessão inexistente");
    }

    @Test
    void start_whenValid_succeeds() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());

        ClassSession started = classSessionRepository.findById(created.getId()).orElseThrow();
        assertThat(started.getStatus()).isEqualTo(ClassSessionStatus.IN_PROGRESS);
    }

    @Test
    void start_whenSessionNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> classSessionService.start(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sessão inexistente");
    }

    @Test
    void start_whenSessionCancelled_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.cancel(created.getId());

        assertThatThrownBy(() -> classSessionService.start(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula está cancelada.");
    }

    @Test
    void start_whenSessionCompleted_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());
        classSessionService.complete(created.getId());

        assertThatThrownBy(() -> classSessionService.start(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula já foi concluída.");
    }

    @Test
    void start_whenSessionAlreadyInProgress_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());

        assertThatThrownBy(() -> classSessionService.start(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula já está em andamento.");
    }

    @Test
    void complete_whenValid_succeeds() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());
        classSessionService.complete(created.getId());

        ClassSession completed = classSessionRepository.findById(created.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(ClassSessionStatus.COMPLETED);
    }

    @Test
    void complete_whenSessionNotStarted_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        assertThatThrownBy(() -> classSessionService.complete(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula precisa ser iniciada antes de ser concluída.");
    }

    @Test
    void complete_whenSessionNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> classSessionService.complete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Sessão inexistente");
    }

    @Test
    void complete_whenSessionCancelled_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.cancel(created.getId());

        assertThatThrownBy(() -> classSessionService.complete(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula está cancelada.");
    }

    @Test
    void complete_whenSessionAlreadyCompleted_throwsConflictException() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());
        classSessionService.complete(created.getId());

        assertThatThrownBy(() -> classSessionService.complete(created.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A aula já foi concluída.");
    }

    @Test
    void findAll_withNoFilters_returnsAllSessions() {
        ClassSessionRequest request1 = new ClassSessionRequest();
        request1.setClassGroupId(classGroup.getId());
        request1.setSessionDate(LocalDate.now());
        classSessionService.create(request1);

        ClassSessionRequest request2 = new ClassSessionRequest();
        request2.setClassGroupId(classGroup.getId());
        request2.setSessionDate(LocalDate.now().plusDays(7));
        classSessionService.create(request2);

        List<ClassSessionResponse> result = classSessionService.findAll(null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_withStatusFilter_returnsFilteredSessions() {
        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(LocalDate.now());
        ClassSessionResponse created = classSessionService.create(request);

        classSessionService.start(created.getId());
        classSessionService.complete(created.getId());

        ClassSessionRequest request2 = new ClassSessionRequest();
        request2.setClassGroupId(classGroup.getId());
        request2.setSessionDate(LocalDate.now().plusDays(7));
        classSessionService.create(request2);

        List<ClassSessionResponse> result = classSessionService.findAll(null, null, ClassSessionStatus.COMPLETED, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ClassSessionStatus.COMPLETED);
    }

    @Test
    void findAll_withSessionDateFilter_returnsFilteredSessions() {
        ClassSessionRequest request1 = new ClassSessionRequest();
        request1.setClassGroupId(classGroup.getId());
        request1.setSessionDate(LocalDate.now());
        classSessionService.create(request1);

        ClassSessionRequest request2 = new ClassSessionRequest();
        request2.setClassGroupId(classGroup.getId());
        request2.setSessionDate(LocalDate.now().plusDays(7));
        classSessionService.create(request2);

        List<ClassSessionResponse> result = classSessionService.findAll(null, null, null, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void findAll_withClassGroupFilter_returnsFilteredSessions() {
        ClassSessionRequest request1 = new ClassSessionRequest();
        request1.setClassGroupId(classGroup.getId());
        request1.setSessionDate(LocalDate.now());
        classSessionService.create(request1);

        List<ClassSessionResponse> result = classSessionService.findAll(classGroup.getId(), null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassGroupId()).isEqualTo(classGroup.getId());
    }

    @Test
    void generateSessions_whenValid_createsSessionsForActiveDays() {
        SessionGenerationResponse result = classSessionService.generateSessions(classGroup.getId());

        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == java.time.DayOfWeek.MONDAY)
                .count();

        assertThat(result.getCreated()).isEqualTo((int) expectedMondays);
        assertThat(result.getIgnored()).isZero();
    }

    @Test
    void generateSessions_whenClassGroupNotFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> classSessionService.generateSessions(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Turma inexistente");
    }

    @Test
    void generateSessions_whenClassGroupInactive_throwsConflictException() {
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        assertThatThrownBy(() -> classSessionService.generateSessions(classGroup.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Não é possível gerar sessões para uma turma inativa.");
    }

    @Test
    void generateSessions_whenInstructorInactive_throwsConflictException() {
        instructor.setActive(false);
        instructorRepository.save(instructor);

        assertThatThrownBy(() -> classSessionService.generateSessions(classGroup.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Instrutor inativo");
    }

    @Test
    void generateSessions_ignoresExistingSessions() {
        LocalDate firstMonday = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == java.time.DayOfWeek.MONDAY)
                .findFirst().orElseThrow();

        ClassSessionRequest request = new ClassSessionRequest();
        request.setClassGroupId(classGroup.getId());
        request.setSessionDate(firstMonday);
        classSessionService.create(request);

        SessionGenerationResponse result = classSessionService.generateSessions(classGroup.getId());

        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == java.time.DayOfWeek.MONDAY)
                .count();

        assertThat(result.getCreated()).isEqualTo((int) expectedMondays - 1);
        assertThat(result.getIgnored()).isEqualTo(1);
    }

    @Test
    void generateSessions_respects60DayWindow() {
        classGroup.setMonday(true);
        classGroup.setTuesday(true);
        classGroup.setWednesday(true);
        classGroup.setThursday(true);
        classGroup.setFriday(true);
        classGroup.setSaturday(true);
        classGroup.setSunday(true);
        classGroupRepository.save(classGroup);

        SessionGenerationResponse result = classSessionService.generateSessions(classGroup.getId());

        assertThat(result.getCreated()).isEqualTo(61);
        assertThat(result.getIgnored()).isZero();
    }
}
