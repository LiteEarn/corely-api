package br.com.corely.instructor;

import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.instructor.dto.InstructorRequest;
import br.com.corely.instructor.dto.InstructorResponse;
import br.com.corely.instructor.dto.TransferClassGroupsRequest;
import br.com.corely.instructor.dto.ReassignResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/instructors")
@RequiredArgsConstructor
public class InstructorController {

    private final InstructorService instructorService;

    @PostMapping
    public ResponseEntity<InstructorResponse> create(@Valid @RequestBody InstructorRequest request) {
        InstructorResponse response = instructorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InstructorResponse>> findAll() {
        List<InstructorResponse> response = instructorService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstructorResponse> findById(@PathVariable UUID id) {
        InstructorResponse response = instructorService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InstructorResponse> update(@PathVariable UUID id, @Valid @RequestBody InstructorRequest request) {
        InstructorResponse response = instructorService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        instructorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sourceInstructorId}/reassign")
    public ResponseEntity<ReassignResponse> reassign(@PathVariable UUID sourceInstructorId, @Valid @RequestBody TransferClassGroupsRequest request) {
        ReassignResponse response = instructorService.reassign(sourceInstructorId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{instructorId}/class-groups")
    public ResponseEntity<List<ClassGroupResponse>> getClassGroupsByInstructorId(@PathVariable UUID instructorId) {
        List<ClassGroupResponse> response = instructorService.getClassGroupsByInstructorId(instructorId);
        return ResponseEntity.ok(response);
    }
}
