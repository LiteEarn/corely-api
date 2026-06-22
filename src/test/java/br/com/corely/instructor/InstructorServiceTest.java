package br.com.corely.instructor;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.instructor.dto.InstructorRequest;
import br.com.corely.instructor.dto.InstructorResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InstructorServiceTest {

    @Autowired
    private InstructorService instructorService;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;

    @BeforeEach
    void setUp() {
        studio = new Studio();
        studio.setName("Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("test@example.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName("Test Class Group");
        classGroup.setStartTime(LocalTime.of(9, 0));
        classGroup.setEndTime(LocalTime.of(10, 0));
        classGroup.setCapacity(20);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        classGroup = classGroupRepository.save(classGroup);
    }

    @Test
    void update_whenInstructorHasActiveClassGroups_throwsBusinessException() {
        // Given
        InstructorRequest request = new InstructorRequest();
        request.setStudioId(studio.getId());
        request.setFullName("Test Instructor");
        request.setEmail("test@example.com");
        request.setActive(false);

        // When & Then
        assertThatThrownBy(() -> instructorService.update(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível inativar o instrutor Test Instructor")
                .hasMessageContaining("Turmas ativas encontradas")
                .hasMessageContaining("Test Class Group");
    }

    @Test
    void update_whenInstructorHasNoActiveClassGroups_canBeInactivated() {
        // Given - deactivate the class group first
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        InstructorRequest request = new InstructorRequest();
        request.setStudioId(studio.getId());
        request.setFullName("Test Instructor");
        request.setEmail("test@example.com");
        request.setActive(false);

        // When
        InstructorResponse response = instructorService.update(instructor.getId(), request);

        // Then
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void update_whenInstructorIsActive_doesNotThrowException() {
        // Given
        InstructorRequest request = new InstructorRequest();
        request.setStudioId(studio.getId());
        request.setFullName("Updated Instructor");
        request.setEmail("updated@example.com");
        request.setActive(true);

        // When
        InstructorResponse response = instructorService.update(instructor.getId(), request);

        // Then
        assertThat(response.getActive()).isTrue();
        assertThat(response.getFullName()).isEqualTo("Updated Instructor");
    }

    @Test
    void update_whenInstructorIsInactivated_withMultipleActiveClassGroups_throwsBusinessExceptionWithAllNames() {
        // Given - create a second active class group
        ClassGroup classGroup2 = new ClassGroup();
        classGroup2.setStudio(studio);
        classGroup2.setInstructor(instructor);
        classGroup2.setName("Test Class Group 2");
        classGroup2.setStartTime(LocalTime.of(11, 0));
        classGroup2.setEndTime(LocalTime.of(12, 0));
        classGroup2.setCapacity(15);
        classGroup2.setTuesday(true);
        classGroup2.setActive(true);
        classGroup2 = classGroupRepository.save(classGroup2);

        InstructorRequest request = new InstructorRequest();
        request.setStudioId(studio.getId());
        request.setFullName("Test Instructor");
        request.setEmail("test@example.com");
        request.setActive(false);

        // When & Then
        assertThatThrownBy(() -> instructorService.update(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível inativar o instrutor Test Instructor")
                .hasMessageContaining("Test Class Group")
                .hasMessageContaining("Test Class Group 2");
    }

    @Test
    void update_whenInstructorHasMixedActiveAndInactiveClassGroups_throwsBusinessExceptionWithOnlyActive() {
        // Given - create an inactive class group
        ClassGroup inactiveClassGroup = new ClassGroup();
        inactiveClassGroup.setStudio(studio);
        inactiveClassGroup.setInstructor(instructor);
        inactiveClassGroup.setName("Inactive Class Group");
        inactiveClassGroup.setStartTime(LocalTime.of(13, 0));
        inactiveClassGroup.setEndTime(LocalTime.of(14, 0));
        inactiveClassGroup.setCapacity(10);
        inactiveClassGroup.setWednesday(true);
        inactiveClassGroup.setActive(false);
        inactiveClassGroup = classGroupRepository.save(inactiveClassGroup);

        InstructorRequest request = new InstructorRequest();
        request.setStudioId(studio.getId());
        request.setFullName("Test Instructor");
        request.setEmail("test@example.com");
        request.setActive(false);

        // When & Then
        assertThatThrownBy(() -> instructorService.update(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível inativar o instrutor Test Instructor")
                .hasMessageContaining("Test Class Group");
    }
}
