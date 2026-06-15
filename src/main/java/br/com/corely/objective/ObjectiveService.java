package br.com.corely.objective;

import br.com.corely.objective.dto.ObjectiveRequest;
import br.com.corely.objective.dto.ObjectiveResponse;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ObjectiveService {

    private final ObjectiveRepository objectiveRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public ObjectiveResponse create(UUID studentId, ObjectiveRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Objective objective = new Objective();
        objective.setStudent(student);
        objective.setType(request.getType());
        objective.setDescription(request.getDescription());
        objective.setStartDate(request.getStartDate());
        objective.setActive(true);

        objective = objectiveRepository.save(objective);
        return toResponse(objective);
    }

    @Transactional(readOnly = true)
    public List<ObjectiveResponse> findByStudentId(UUID studentId) {
        return objectiveRepository.findByStudentId(studentId).stream()
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

        objective.setType(request.getType());
        objective.setDescription(request.getDescription());
        objective.setStartDate(request.getStartDate());

        objective = objectiveRepository.save(objective);
        return toResponse(objective);
    }

    @Transactional
    public void delete(UUID id) {
        Objective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Objective not found"));
        objectiveRepository.delete(objective);
    }

    private ObjectiveResponse toResponse(Objective objective) {
        return new ObjectiveResponse(
                objective.getId(),
                objective.getStudent().getId(),
                objective.getType(),
                objective.getDescription(),
                objective.getStartDate(),
                objective.getActive()
        );
    }
}
