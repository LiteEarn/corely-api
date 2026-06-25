package br.com.corely.instructor;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.instructor.dto.InstructorRequest;
import br.com.corely.instructor.dto.InstructorResponse;
import br.com.corely.instructor.dto.ReassignRequest;
import br.com.corely.instructor.dto.ReassignResponse;
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

    @Test
    void reassign_whenInstructorsExistAndTargetActive_transfersActiveClassGroups() {
        // Given - create target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

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

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(null); // Transfer all (backward compatibility)

        // When
        ReassignResponse response = instructorService.reassign(instructor.getId(), request);

        // Then
        assertThat(response.getUpdatedCount()).isEqualTo(2);

        List<ClassGroup> sourceInstructorClassGroups = classGroupRepository.findByInstructorIdAndActiveTrue(instructor.getId());
        assertThat(sourceInstructorClassGroups).isEmpty();

        List<ClassGroup> targetInstructorClassGroups = classGroupRepository.findByInstructorIdAndActiveTrue(targetInstructor.getId());
        assertThat(targetInstructorClassGroups).hasSize(2);
    }

    @Test
    void reassign_whenClassGroupIdsProvided_transfersOnlySelectedClassGroups() {
        // Given - create target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

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

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(List.of(classGroup.getId())); // Transfer only first class group

        // When
        ReassignResponse response = instructorService.reassign(instructor.getId(), request);

        // Then
        assertThat(response.getUpdatedCount()).isEqualTo(1);

        List<ClassGroup> sourceInstructorClassGroups = classGroupRepository.findByInstructorIdAndActiveTrue(instructor.getId());
        assertThat(sourceInstructorClassGroups).hasSize(1);
        assertThat(sourceInstructorClassGroups.get(0).getName()).isEqualTo("Test Class Group 2");

        List<ClassGroup> targetInstructorClassGroups = classGroupRepository.findByInstructorIdAndActiveTrue(targetInstructor.getId());
        assertThat(targetInstructorClassGroups).hasSize(1);
        assertThat(targetInstructorClassGroups.get(0).getName()).isEqualTo("Test Class Group");
    }

    @Test
    void reassign_whenClassGroupNotFound_throwsResourceNotFoundException() {
        // Given - create target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(List.of(UUID.randomUUID()));

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(instructor.getId(), request))
                .hasMessageContaining("Class group not found");
    }

    @Test
    void reassign_whenClassGroupNotBelongToSourceInstructor_throwsBusinessException() {
        // Given - create target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

        // Given - create class group for another instructor
        Instructor otherInstructor = new Instructor();
        otherInstructor.setStudio(studio);
        otherInstructor.setFullName("Other Instructor");
        otherInstructor.setEmail("other@example.com");
        otherInstructor.setActive(true);
        otherInstructor = instructorRepository.save(otherInstructor);

        ClassGroup otherClassGroup = new ClassGroup();
        otherClassGroup.setStudio(studio);
        otherClassGroup.setInstructor(otherInstructor);
        otherClassGroup.setName("Other Class Group");
        otherClassGroup.setStartTime(LocalTime.of(11, 0));
        otherClassGroup.setEndTime(LocalTime.of(12, 0));
        otherClassGroup.setCapacity(15);
        otherClassGroup.setTuesday(true);
        otherClassGroup.setActive(true);
        otherClassGroup = classGroupRepository.save(otherClassGroup);

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(List.of(otherClassGroup.getId()));

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Class group does not belong to source instructor");
    }

    @Test
    void reassign_whenClassGroupInactive_throwsBusinessException() {
        // Given - create target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

        // Given - create inactive class group
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

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(List.of(inactiveClassGroup.getId()));

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Class group is not active");
    }

    @Test
    void reassign_whenSourceInstructorNotFound_throwsResourceNotFoundException() {
        // Given
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(null);

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(UUID.randomUUID(), request))
                .hasMessageContaining("Source instructor not found");
    }

    @Test
    void reassign_whenTargetInstructorNotFound_throwsResourceNotFoundException() {
        // Given
        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(UUID.randomUUID());
        request.setClassGroupIds(null);

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(instructor.getId(), request))
                .hasMessageContaining("Target instructor not found");
    }

    @Test
    void reassign_whenTargetInstructorInactive_throwsBusinessException() {
        // Given - create inactive target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(false);
        targetInstructor = instructorRepository.save(targetInstructor);

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(null);

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Target instructor must be active");
    }

    @Test
    void reassign_whenSourceAndTargetAreSame_throwsBusinessException() {
        // Given
        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(instructor.getId());
        request.setClassGroupIds(null);

        // When & Then
        assertThatThrownBy(() -> instructorService.reassign(instructor.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Source and target instructors cannot be the same");
    }

    @Test
    void reassign_whenNoActiveClassGroups_returnsZero() {
        // Given - deactivate the class group
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        // Given - create target instructor
        Instructor targetInstructor = new Instructor();
        targetInstructor.setStudio(studio);
        targetInstructor.setFullName("Target Instructor");
        targetInstructor.setEmail("target@example.com");
        targetInstructor.setActive(true);
        targetInstructor = instructorRepository.save(targetInstructor);

        ReassignRequest request = new ReassignRequest();
        request.setTargetInstructorId(targetInstructor.getId());
        request.setClassGroupIds(null);

        // When
        ReassignResponse response = instructorService.reassign(instructor.getId(), request);

        // Then
        assertThat(response.getUpdatedCount()).isEqualTo(0);
    }

    @Test
    void getClassGroupsByInstructorId_whenInstructorExists_returnsOnlyActiveClassGroups() {
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

        // When
        List<ClassGroupResponse> response = instructorService.getClassGroupsByInstructorId(instructor.getId());

        // Then
        assertThat(response).hasSize(2);
        assertThat(response).allMatch(ClassGroupResponse::getActive);
        assertThat(response).extracting("name")
                .containsExactlyInAnyOrder("Test Class Group", "Test Class Group 2");
    }

    @Test
    void getClassGroupsByInstructorId_whenInstructorNotFound_throwsResourceNotFoundException() {
        // When & Then
        assertThatThrownBy(() -> instructorService.getClassGroupsByInstructorId(UUID.randomUUID()))
                .hasMessageContaining("Instructor not found");
    }

    @Test
    void getClassGroupsByInstructorId_whenInstructorHasNoActiveClassGroups_returnsEmptyList() {
        // Given - deactivate the class group
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        // When
        List<ClassGroupResponse> response = instructorService.getClassGroupsByInstructorId(instructor.getId());

        // Then
        assertThat(response).isEmpty();
    }

    @Test
    void getClassGroupsByInstructorId_whenDifferentInstructors_returnsOnlyTheirClassGroups() {
        // Given - create second instructor with their own class group
        Instructor instructor2 = new Instructor();
        instructor2.setStudio(studio);
        instructor2.setFullName("Instructor 2");
        instructor2.setEmail("instructor2@example.com");
        instructor2.setActive(true);
        instructor2 = instructorRepository.save(instructor2);

        ClassGroup classGroup2 = new ClassGroup();
        classGroup2.setStudio(studio);
        classGroup2.setInstructor(instructor2);
        classGroup2.setName("Instructor 2 Class Group");
        classGroup2.setStartTime(LocalTime.of(11, 0));
        classGroup2.setEndTime(LocalTime.of(12, 0));
        classGroup2.setCapacity(15);
        classGroup2.setTuesday(true);
        classGroup2.setActive(true);
        classGroup2 = classGroupRepository.save(classGroup2);

        // When
        List<ClassGroupResponse> response1 = instructorService.getClassGroupsByInstructorId(instructor.getId());
        List<ClassGroupResponse> response2 = instructorService.getClassGroupsByInstructorId(instructor2.getId());

        // Then
        assertThat(response1).hasSize(1);
        assertThat(response1.get(0).getName()).isEqualTo("Test Class Group");

        assertThat(response2).hasSize(1);
        assertThat(response2.get(0).getName()).isEqualTo("Instructor 2 Class Group");
    }
}
