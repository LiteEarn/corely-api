package br.com.corely.evaluation;

import br.com.corely.evaluation.dto.EvaluationRequest;
import br.com.corely.evaluation.dto.EvaluationResponse;
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
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final StudioRepository studioRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public EvaluationResponse create(EvaluationRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Evaluation evaluation = new Evaluation();
        evaluation.setStudio(studio);
        evaluation.setStudent(student);
        evaluation.setEvaluationDate(request.getEvaluationDate());
        evaluation.setWeight(request.getWeight());
        evaluation.setHeight(request.getHeight());
        evaluation.setObservations(request.getObservations());

        evaluation = evaluationRepository.save(evaluation);
        return toResponse(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> findAll() {
        return evaluationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvaluationResponse findById(UUID id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
        return toResponse(evaluation);
    }

    @Transactional
    public EvaluationResponse update(UUID id, EvaluationRequest request) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        evaluation.setStudio(studio);
        evaluation.setStudent(student);
        evaluation.setEvaluationDate(request.getEvaluationDate());
        evaluation.setWeight(request.getWeight());
        evaluation.setHeight(request.getHeight());
        evaluation.setObservations(request.getObservations());

        evaluation = evaluationRepository.save(evaluation);
        return toResponse(evaluation);
    }

    @Transactional
    public void delete(UUID id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found"));
        evaluationRepository.delete(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> findByStudentId(UUID studentId) {
        return evaluationRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> findByEvaluationDateBetween(LocalDate startDate, LocalDate endDate) {
        return evaluationRepository.findByEvaluationDateBetween(startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> findByStudentIdAndEvaluationDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate) {
        return evaluationRepository.findByStudentIdAndEvaluationDateBetween(studentId, startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EvaluationResponse toResponse(Evaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getStudio().getId(),
                evaluation.getStudent().getId(),
                evaluation.getStudent().getFullName(),
                evaluation.getEvaluationDate(),
                evaluation.getWeight(),
                evaluation.getHeight(),
                evaluation.getObservations(),
                evaluation.getCreatedAt(),
                evaluation.getUpdatedAt()
        );
    }
}
