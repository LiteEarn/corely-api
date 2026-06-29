package br.com.corely.classsession;

import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/class-sessions")
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @PostMapping
    public ResponseEntity<ClassSessionResponse> create(@Valid @RequestBody ClassSessionRequest request) {
        ClassSessionResponse response = classSessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClassSessionResponse>> findAll() {
        List<ClassSessionResponse> response = classSessionService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassSessionResponse> findById(@PathVariable UUID id) {
        ClassSessionResponse response = classSessionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        classSessionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
