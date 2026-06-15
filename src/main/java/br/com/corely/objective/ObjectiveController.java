package br.com.corely.objective;

import br.com.corely.objective.dto.ObjectiveRequest;
import br.com.corely.objective.dto.ObjectiveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    @PostMapping("/students/{studentId}/objectives")
    public ResponseEntity<ObjectiveResponse> create(
            @PathVariable UUID studentId,
            @Valid @RequestBody ObjectiveRequest request) {
        ObjectiveResponse response = objectiveService.create(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/students/{studentId}/objectives")
    public ResponseEntity<List<ObjectiveResponse>> findByStudentId(@PathVariable UUID studentId) {
        List<ObjectiveResponse> response = objectiveService.findByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/objectives/{id}")
    public ResponseEntity<ObjectiveResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ObjectiveRequest request) {
        ObjectiveResponse response = objectiveService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/objectives/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        objectiveService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
