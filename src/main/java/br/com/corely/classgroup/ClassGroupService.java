package br.com.corely.classgroup;

import br.com.corely.classgroup.dto.ClassGroupRequest;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassGroupService {

    private final ClassGroupRepository classGroupRepository;
    private final StudioRepository studioRepository;
    private final InstructorRepository instructorRepository;

    @Transactional
    public ClassGroupResponse create(ClassGroupRequest request) {
        validateAtLeastOneDay(request);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Instructor instructor = instructorRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        validateInstructorActive(instructor);

        ClassGroup classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName(request.getName());
        classGroup.setDescription(request.getDescription());
        classGroup.setStartTime(request.getStartTime());
        classGroup.setEndTime(request.getEndTime());
        classGroup.setCapacity(request.getCapacity());
        classGroup.setMonday(request.getMonday() != null ? request.getMonday() : false);
        classGroup.setTuesday(request.getTuesday() != null ? request.getTuesday() : false);
        classGroup.setWednesday(request.getWednesday() != null ? request.getWednesday() : false);
        classGroup.setThursday(request.getThursday() != null ? request.getThursday() : false);
        classGroup.setFriday(request.getFriday() != null ? request.getFriday() : false);
        classGroup.setSaturday(request.getSaturday() != null ? request.getSaturday() : false);
        classGroup.setSunday(request.getSunday() != null ? request.getSunday() : false);
        classGroup.setActive(request.getActive() != null ? request.getActive() : true);

        classGroup = classGroupRepository.save(classGroup);
        return toResponse(classGroup);
    }

    @Transactional(readOnly = true)
    public List<ClassGroupResponse> findAll() {
        return classGroupRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClassGroupResponse> findActive() {
        return classGroupRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassGroupResponse findById(UUID id) {
        ClassGroup classGroup = classGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));
        return toResponse(classGroup);
    }

    @Transactional
    public ClassGroupResponse update(UUID id, ClassGroupRequest request) {
        validateAtLeastOneDay(request);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        ClassGroup classGroup = classGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Instructor instructor = instructorRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        validateInstructorActive(instructor);

        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName(request.getName());
        classGroup.setDescription(request.getDescription());
        classGroup.setStartTime(request.getStartTime());
        classGroup.setEndTime(request.getEndTime());
        classGroup.setCapacity(request.getCapacity());
        if (request.getMonday() != null) {
            classGroup.setMonday(request.getMonday());
        }
        if (request.getTuesday() != null) {
            classGroup.setTuesday(request.getTuesday());
        }
        if (request.getWednesday() != null) {
            classGroup.setWednesday(request.getWednesday());
        }
        if (request.getThursday() != null) {
            classGroup.setThursday(request.getThursday());
        }
        if (request.getFriday() != null) {
            classGroup.setFriday(request.getFriday());
        }
        if (request.getSaturday() != null) {
            classGroup.setSaturday(request.getSaturday());
        }
        if (request.getSunday() != null) {
            classGroup.setSunday(request.getSunday());
        }
        if (request.getActive() != null) {
            classGroup.setActive(request.getActive());
        }

        classGroup = classGroupRepository.save(classGroup);
        return toResponse(classGroup);
    }

    @Transactional
    public void delete(UUID id) {
        ClassGroup classGroup = classGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));
        classGroupRepository.delete(classGroup);
    }

    private ClassGroupResponse toResponse(ClassGroup classGroup) {
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
                classGroup.getActive(),
                classGroup.getCreatedAt(),
                classGroup.getUpdatedAt()
        );
    }

    private void validateAtLeastOneDay(ClassGroupRequest request) {
        Boolean monday = request.getMonday() != null ? request.getMonday() : false;
        Boolean tuesday = request.getTuesday() != null ? request.getTuesday() : false;
        Boolean wednesday = request.getWednesday() != null ? request.getWednesday() : false;
        Boolean thursday = request.getThursday() != null ? request.getThursday() : false;
        Boolean friday = request.getFriday() != null ? request.getFriday() : false;
        Boolean saturday = request.getSaturday() != null ? request.getSaturday() : false;
        Boolean sunday = request.getSunday() != null ? request.getSunday() : false;

        if (!monday && !tuesday && !wednesday && !thursday && !friday && !saturday && !sunday) {
            throw new BusinessException("Selecione pelo menos um dia da semana para a turma.");
        }
    }

    private void validateTimeRange(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException("A hora de término deve ser maior que a hora de início.");
        }
    }

    private void validateInstructorActive(Instructor instructor) {
        if (!Boolean.TRUE.equals(instructor.getActive())) {
            throw new BusinessException("Selected instructor is inactive.");
        }
    }
}
