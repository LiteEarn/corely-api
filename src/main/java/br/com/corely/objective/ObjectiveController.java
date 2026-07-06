package br.com.corely.objective;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.objective.dto.ObjectiveRequest;
import br.com.corely.objective.dto.ObjectiveResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/objectives")
@RequiredArgsConstructor
@Tag(name = "Objectives", description = "Objective management endpoints")
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    @PostMapping
    @RequireRole({UserRole.INSTRUCTOR, UserRole.ADMIN})
    @Operation(summary = "Create a new objective")
    public ResponseEntity<ObjectiveResponse> create(@Valid @RequestBody ObjectiveRequest request) {
        ObjectiveResponse response = objectiveService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.INSTRUCTOR, UserRole.ADMIN})
    @Operation(summary = "Get all objectives with optional filters")
    public ResponseEntity<List<ObjectiveResponse>> findAll(
            @Parameter(description = "Filter by student ID")
            @RequestParam(required = false) UUID studentId,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) ObjectiveStatus status,
            @Parameter(description = "Search in title")
            @RequestParam(required = false) String search) {
        List<ObjectiveResponse> response = objectiveService.findAll(studentId, status, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.INSTRUCTOR, UserRole.ADMIN})
    @Operation(summary = "Get objective by ID")
    public ResponseEntity<ObjectiveResponse> findById(@PathVariable UUID id) {
        ObjectiveResponse response = objectiveService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.INSTRUCTOR, UserRole.ADMIN})
    @Operation(summary = "Update an objective")
    public ResponseEntity<ObjectiveResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ObjectiveRequest request) {
        ObjectiveResponse response = objectiveService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({UserRole.INSTRUCTOR, UserRole.ADMIN})
    @Operation(summary = "Delete an objective")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        objectiveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
