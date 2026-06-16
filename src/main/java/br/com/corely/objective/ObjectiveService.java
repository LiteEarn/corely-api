package br.com.corely.objective;

import br.com.corely.objective.dto.ObjectiveRequest;
import br.com.corely.objective.dto.ObjectiveResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObjectiveService {

    private final ObjectiveRepository objectiveRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;

    @Transactional
    public ObjectiveResponse create(ObjectiveRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        validateTargetDate(request.getStartDate(), request.getTargetDate());

        Objective objective = new Objective();
        objective.setStudio(studio);
        objective.setStudent(student);
        objective.setTitle(request.getTitle());
        objective.setDescription(request.getDescription());
        objective.setStatus(request.getStatus());
        objective.setStartDate(request.getStartDate());
        objective.setTargetDate(request.getTargetDate());

        objective = objectiveRepository.save(objective);
        return toResponse(objective);
    }

    @Transactional(readOnly = true)
    public List<ObjectiveResponse> findAll(UUID studentId, ObjectiveStatus status, String search) {
        return objectiveRepository.findAllWithFilters(studentId, status, search).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ObjectiveResponse findById(UUID id) {
        Objective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objective not found"));
        return toResponse(objective);
    }

    @Transactional
    public ObjectiveResponse update(UUID id, ObjectiveRequest request) {
        Objective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objective not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        validateTargetDate(request.getStartDate(), request.getTargetDate());

        objective.setStudio(studio);
        objective.setStudent(student);
        objective.setTitle(request.getTitle());
        objective.setDescription(request.getDescription());
        objective.setStatus(request.getStatus());
        objective.setStartDate(request.getStartDate());
        objective.setTargetDate(request.getTargetDate());

        objective = objectiveRepository.save(objective);
        return toResponse(objective);
    }

    @Transactional
    public void delete(UUID id) {
        Objective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objective not found"));
        objectiveRepository.delete(objective);
    }

    private void validateTargetDate(LocalDate startDate, LocalDate targetDate) {
        if (targetDate != null && targetDate.isBefore(startDate)) {
            throw new BusinessException("Target date must be greater than or equal to start date");
        }
    }

    private ObjectiveResponse toResponse(Objective objective) {
        return new ObjectiveResponse(
                objective.getId(),
                objective.getStudio().getId(),
                objective.getStudent().getId(),
                objective.getTitle(),
                objective.getDescription(),
                objective.getStatus(),
                objective.getStartDate(),
                objective.getTargetDate(),
                objective.getCreatedAt(),
                objective.getUpdatedAt()
        );
    }
}
