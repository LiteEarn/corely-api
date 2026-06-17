package br.com.corely.evolution;

import br.com.corely.evaluation.Evaluation;
import br.com.corely.evaluation.EvaluationRepository;
import br.com.corely.evolution.dto.EvolutionRequest;
import br.com.corely.evolution.dto.EvolutionResponse;
import br.com.corely.objective.Objective;
import br.com.corely.objective.ObjectiveRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvolutionService {

    private final EvolutionRepository evolutionRepository;
    private final StudioRepository studioRepository;
    private final StudentRepository studentRepository;
    private final ObjectiveRepository objectiveRepository;
    private final EvaluationRepository evaluationRepository;

    @Transactional
    public EvolutionResponse create(EvolutionRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Objective objective = null;
        if (request.getObjectiveId() != null) {
            objective = objectiveRepository.findById(request.getObjectiveId())
                    .orElseThrow(() -> new ResourceNotFoundException("Objective not found"));
        }

        Evaluation evaluation = null;
        if (request.getEvaluationId() != null) {
            evaluation = evaluationRepository.findById(request.getEvaluationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = authentication != null ? authentication.getName() : "system";

        Evolution evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setObjective(objective);
        evolution.setEvaluation(evaluation);
        evolution.setEvolutionDate(request.getEvolutionDate());
        evolution.setTitle(request.getTitle());
        evolution.setDescription(request.getDescription());
        evolution.setCreatedBy(createdBy);

        evolution = evolutionRepository.save(evolution);
        return toResponse(evolution);
    }

    @Transactional(readOnly = true)
    public List<EvolutionResponse> findAll() {
        return evolutionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvolutionResponse findById(UUID id) {
        Evolution evolution = evolutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evolution not found"));
        return toResponse(evolution);
    }

    @Transactional
    public EvolutionResponse update(UUID id, EvolutionRequest request) {
        Evolution evolution = evolutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evolution not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Objective objective = null;
        if (request.getObjectiveId() != null) {
            objective = objectiveRepository.findById(request.getObjectiveId())
                    .orElseThrow(() -> new ResourceNotFoundException("Objective not found"));
        }

        Evaluation evaluation = null;
        if (request.getEvaluationId() != null) {
            evaluation = evaluationRepository.findById(request.getEvaluationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = authentication != null ? authentication.getName() : "system";

        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setObjective(objective);
        evolution.setEvaluation(evaluation);
        evolution.setEvolutionDate(request.getEvolutionDate());
        evolution.setTitle(request.getTitle());
        evolution.setDescription(request.getDescription());
        evolution.setCreatedBy(createdBy);

        evolution = evolutionRepository.save(evolution);
        return toResponse(evolution);
    }

    @Transactional
    public void delete(UUID id) {
        Evolution evolution = evolutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evolution not found"));
        evolutionRepository.delete(evolution);
    }

    @Transactional(readOnly = true)
    public List<EvolutionResponse> findByStudentId(UUID studentId) {
        return evolutionRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvolutionResponse> findByObjectiveId(UUID objectiveId) {
        return evolutionRepository.findByObjectiveId(objectiveId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvolutionResponse> findByEvolutionDateBetween(LocalDate startDate, LocalDate endDate) {
        return evolutionRepository.findByEvolutionDateBetween(startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvolutionResponse> findByStudentIdAndEvolutionDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate) {
        return evolutionRepository.findByStudentIdAndEvolutionDateBetween(studentId, startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvolutionResponse> findByObjectiveIdAndEvolutionDateBetween(UUID objectiveId, LocalDate startDate, LocalDate endDate) {
        return evolutionRepository.findByObjectiveIdAndEvolutionDateBetween(objectiveId, startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EvolutionResponse toResponse(Evolution evolution) {
        return new EvolutionResponse(
                evolution.getId(),
                evolution.getStudio().getId(),
                evolution.getStudent().getId(),
                evolution.getStudent().getFullName(),
                evolution.getObjective() != null ? evolution.getObjective().getId() : null,
                evolution.getObjective() != null ? evolution.getObjective().getTitle() : null,
                evolution.getEvaluation() != null ? evolution.getEvaluation().getId() : null,
                evolution.getEvolutionDate(),
                evolution.getTitle(),
                evolution.getDescription(),
                evolution.getCreatedBy(),
                evolution.getCreatedAt(),
                evolution.getUpdatedAt()
        );
    }
}
