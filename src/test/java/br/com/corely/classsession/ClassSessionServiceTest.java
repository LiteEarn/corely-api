package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
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
                .hasMessage("Sessão já está cancelada");
    }
}
