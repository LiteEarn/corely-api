package br.com.corely.evaluation;

import br.com.corely.evaluation.dto.EvaluationRequest;
import br.com.corely.evaluation.dto.EvaluationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluations", description = "API for managing student evaluations")
public class EvaluationController {

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);

    private final EvaluationService evaluationService;

    @PostMapping
    @Operation(summary = "Create a new evaluation", description = "Creates a new evaluation for a student")
    public ResponseEntity<EvaluationResponse> create(@Valid @RequestBody EvaluationRequest request) {
        log.info("EvaluationController.create - Request received");
        EvaluationResponse response = evaluationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all evaluations", description = "Retrieves all evaluations with optional filters")
    public ResponseEntity<List<EvaluationResponse>> findAll(
            @Parameter(description = "Filter by student ID") @RequestParam(required = false) UUID studentId,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        log.info("EvaluationController.findAll - Request received with filters: studentId={}, startDate={}, endDate={}", 
                studentId, startDate, endDate);
        
        List<EvaluationResponse> response;
        
        if (studentId != null && startDate != null && endDate != null) {
            response = evaluationService.findByStudentIdAndEvaluationDateBetween(studentId, startDate, endDate);
        } else if (studentId != null) {
            response = evaluationService.findByStudentId(studentId);
        } else if (startDate != null && endDate != null) {
            response = evaluationService.findByEvaluationDateBetween(startDate, endDate);
        } else {
            response = evaluationService.findAll();
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evaluation by ID", description = "Retrieves a specific evaluation by its ID")
    public ResponseEntity<EvaluationResponse> findById(@PathVariable UUID id) {
        log.info("EvaluationController.findById - Request received for id: {}", id);
        EvaluationResponse response = evaluationService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update evaluation", description = "Updates an existing evaluation")
    public ResponseEntity<EvaluationResponse> update(@PathVariable UUID id, @Valid @RequestBody EvaluationRequest request) {
        log.info("EvaluationController.update - Request received for id: {}", id);
        EvaluationResponse response = evaluationService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete evaluation", description = "Deletes an evaluation by its ID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("EvaluationController.delete - Request received for id: {}", id);
        evaluationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
