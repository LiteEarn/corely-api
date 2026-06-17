package br.com.corely.evolution;

import br.com.corely.evolution.dto.EvolutionRequest;
import br.com.corely.evolution.dto.EvolutionResponse;
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
@RequestMapping("/evolutions")
@RequiredArgsConstructor
@Tag(name = "Evolutions", description = "API for managing student evolutions")
public class EvolutionController {

    private static final Logger log = LoggerFactory.getLogger(EvolutionController.class);

    private final EvolutionService evolutionService;

    @PostMapping
    @Operation(summary = "Create a new evolution", description = "Creates a new evolution for a student")
    public ResponseEntity<EvolutionResponse> create(@Valid @RequestBody EvolutionRequest request) {
        log.info("EvolutionController.create - Request received");
        EvolutionResponse response = evolutionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all evolutions", description = "Retrieves all evolutions with optional filters")
    public ResponseEntity<List<EvolutionResponse>> findAll(
            @Parameter(description = "Filter by student ID") @RequestParam(required = false) UUID studentId,
            @Parameter(description = "Filter by objective ID") @RequestParam(required = false) UUID objectiveId,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        log.info("EvolutionController.findAll - Request received with filters: studentId={}, objectiveId={}, startDate={}, endDate={}",
                studentId, objectiveId, startDate, endDate);

        List<EvolutionResponse> response;

        if (studentId != null && objectiveId != null && startDate != null && endDate != null) {
            response = evolutionService.findByStudentIdAndEvolutionDateBetween(studentId, startDate, endDate);
        } else if (objectiveId != null && startDate != null && endDate != null) {
            response = evolutionService.findByObjectiveIdAndEvolutionDateBetween(objectiveId, startDate, endDate);
        } else if (studentId != null && startDate != null && endDate != null) {
            response = evolutionService.findByStudentIdAndEvolutionDateBetween(studentId, startDate, endDate);
        } else if (studentId != null) {
            response = evolutionService.findByStudentId(studentId);
        } else if (objectiveId != null) {
            response = evolutionService.findByObjectiveId(objectiveId);
        } else if (startDate != null && endDate != null) {
            response = evolutionService.findByEvolutionDateBetween(startDate, endDate);
        } else {
            response = evolutionService.findAll();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evolution by ID", description = "Retrieves a specific evolution by its ID")
    public ResponseEntity<EvolutionResponse> findById(@PathVariable UUID id) {
        log.info("EvolutionController.findById - Request received for id: {}", id);
        EvolutionResponse response = evolutionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update evolution", description = "Updates an existing evolution")
    public ResponseEntity<EvolutionResponse> update(@PathVariable UUID id, @Valid @RequestBody EvolutionRequest request) {
        log.info("EvolutionController.update - Request received for id: {}", id);
        EvolutionResponse response = evolutionService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete evolution", description = "Deletes an evolution by its ID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("EvolutionController.delete - Request received for id: {}", id);
        evolutionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
