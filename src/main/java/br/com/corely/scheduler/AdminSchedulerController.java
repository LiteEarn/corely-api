package br.com.corely.scheduler;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.classsession.dto.SessionGenerationResponse;
import br.com.corely.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/scheduler")
@RequiredArgsConstructor
public class AdminSchedulerController {

    private final SessionGenerationService sessionGenerationService;

    @PostMapping("/generate-sessions")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<SessionGenerationResponse> generateSessions() {
        SessionGenerationResponse response = sessionGenerationService.generateForAllActiveGroups();
        return ResponseEntity.ok(response);
    }
}
