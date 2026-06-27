package br.com.corely.classgroup;

import br.com.corely.classgroup.dto.ClassGroupRequest;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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

    private Studio studio;
    private Instructor activeInstructor;
    private Instructor inactiveInstructor;

    @BeforeEach
    void setUp() {
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
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
}
