package br.com.corely.instructor;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.instructor.dto.InstructorRequest;
import br.com.corely.instructor.dto.InstructorResponse;
import br.com.corely.instructor.dto.TransferClassGroupsRequest;
import br.com.corely.instructor.dto.ReassignResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final StudioRepository studioRepository;
    private final ClassGroupRepository classGroupRepository;

    @Transactional
    public InstructorResponse create(InstructorRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Instructor instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName(request.getFullName());
        instructor.setEmail(request.getEmail());
        instructor.setPhone(request.getPhone());
        instructor.setSpecialty(request.getSpecialty());
        instructor.setActive(true);

        instructor = instructorRepository.save(instructor);
        return toResponse(instructor);
    }

    @Transactional(readOnly = true)
    public List<InstructorResponse> findAll() {
        return instructorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstructorResponse findById(UUID id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
        return toResponse(instructor);
    }

    @Transactional
    public InstructorResponse update(UUID id, InstructorRequest request) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        instructor.setStudio(studio);
        instructor.setFullName(request.getFullName());
        instructor.setEmail(request.getEmail());
        instructor.setPhone(request.getPhone());
        instructor.setSpecialty(request.getSpecialty());
        
        // Check if instructor is being inactivated
        if (request.getActive() != null && Boolean.FALSE.equals(request.getActive()) && Boolean.TRUE.equals(instructor.getActive())) {
            validateInstructorHasNoActiveClassGroups(instructor);
        }

        if (request.getActive() != null) {
            instructor.setActive(request.getActive());
        }

        instructor = instructorRepository.save(instructor);
        return toResponse(instructor);
    }

    @Transactional
    public void delete(UUID id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
        instructorRepository.delete(instructor);
    }

    @Transactional
    public ReassignResponse reassign(UUID sourceInstructorId, TransferClassGroupsRequest request) {
        Instructor sourceInstructor = instructorRepository.findById(sourceInstructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Source instructor not found"));

        Instructor targetInstructor = instructorRepository.findById(request.getTargetInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Target instructor not found"));

        if (!targetInstructor.getActive()) {
            throw new BusinessException("Target instructor must be active");
        }

        if (sourceInstructorId.equals(request.getTargetInstructorId())) {
            throw new BusinessException("Source and target instructors cannot be the same");
        }

        if (request.getClassGroupIds() == null || request.getClassGroupIds().isEmpty()) {
            throw new BusinessException("Class group IDs cannot be empty");
        }

        // Load and validate class groups
        List<ClassGroup> classGroupsToTransfer = new java.util.ArrayList<>();
        UUID sourceStudioId = null;

        for (UUID classGroupId : request.getClassGroupIds()) {
            ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class group not found: " + classGroupId));

            if (!classGroup.getInstructor().getId().equals(sourceInstructorId)) {
                throw new BusinessException("Class group does not belong to source instructor: " + classGroupId);
            }

            if (!classGroup.getActive()) {
                throw new BusinessException("Class group is not active: " + classGroupId);
            }

            // Validate all class groups belong to the same Studio
            if (sourceStudioId == null) {
                sourceStudioId = classGroup.getStudio().getId();
            } else if (!classGroup.getStudio().getId().equals(sourceStudioId)) {
                throw new BusinessException("Class group does not belong to the same Studio: " + classGroupId);
            }

            classGroupsToTransfer.add(classGroup);
        }

        // Update instructor for selected class groups
        for (ClassGroup classGroup : classGroupsToTransfer) {
            classGroup.setInstructor(targetInstructor);
            classGroupRepository.save(classGroup);
        }

        // Log transfer operation
        log.info("Class group transfer completed - sourceInstructorId: {}, targetInstructorId: {}, quantity: {}, classGroupIds: {}",
                sourceInstructorId, request.getTargetInstructorId(), classGroupsToTransfer.size(), request.getClassGroupIds());

        return new ReassignResponse(classGroupsToTransfer.size());
    }

    @Transactional(readOnly = true)
    public List<ClassGroupResponse> getClassGroupsByInstructorId(UUID instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        List<ClassGroup> activeClassGroups = classGroupRepository.findByInstructorIdAndActiveTrue(instructorId);

        return activeClassGroups.stream()
                .map(this::toClassGroupResponse)
                .collect(Collectors.toList());
    }

    private ClassGroupResponse toClassGroupResponse(ClassGroup classGroup) {
        return new ClassGroupResponse(
                classGroup.getId(),
                classGroup.getStudio().getId(),
                classGroup.getInstructor().getId(),
                classGroup.getInstructor().getFullName(),
                classGroup.getName(),
                classGroup.getDescription(),
                classGroup.getStartTime(),
                classGroup.getEndTime(),
                classGroup.getCapacity(),
                classGroup.getMonday(),
                classGroup.getTuesday(),
                classGroup.getWednesday(),
                classGroup.getThursday(),
                classGroup.getFriday(),
                classGroup.getSaturday(),
                classGroup.getSunday(),
                classGroup.getStartDate(),
                classGroup.getEndDate(),
                classGroup.getActive(),
                classGroup.getCreatedAt(),
                classGroup.getUpdatedAt()
        );
    }

    private InstructorResponse toResponse(Instructor instructor) {
        return new InstructorResponse(
                instructor.getId(),
                instructor.getStudio().getId(),
                instructor.getFullName(),
                instructor.getEmail(),
                instructor.getPhone(),
                instructor.getSpecialty(),
                instructor.getActive()
        );
    }

    private void validateInstructorHasNoActiveClassGroups(Instructor instructor) {
        List<ClassGroup> activeClassGroups = classGroupRepository.findByInstructorIdAndActiveTrue(instructor.getId());
        if (!activeClassGroups.isEmpty()) {
            String classGroupNames = activeClassGroups.stream()
                    .map(ClassGroup::getName)
                    .collect(Collectors.joining("\n- "));
            
            throw new BusinessException(
                    String.format(
                            "Não é possível inativar o instrutor %s.\n\n" +
                            "Turmas ativas encontradas:\n" +
                            "- %s\n\n" +
                            "Por favor, transfira estas turmas para outro instrutor antes da inativação.",
                            instructor.getFullName(),
                            classGroupNames
                    )
            );
        }
    }
}
