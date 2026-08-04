package br.com.corely.dev.seed;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de seed de dados de desenvolvimento.
 *
 * <p>Disponível <b>apenas no perfil {@code dev}</b>: em qualquer outro ambiente
 * (incluindo produção) o endpoint não existe e as requisições a
 * {@code /dev/seed/**} são rejeitadas (EPIC-02-S05).</p>
 */
@RestController
@RequestMapping("/dev/seed")
@Profile("dev")
@RequiredArgsConstructor
public class SeedController {

    private final SeedService seedService;

    @PostMapping
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<SeedResponse> seedAll() {
        SeedResponse response = seedService.execute();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dashboard")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Map<String, String>> seedDashboard() {
        seedService.ensureDashboardData();
        return ResponseEntity.ok(Map.of("status", "Dashboard data adjusted"));
    }

    @PostMapping("/students")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Map<String, String>> seedStudents() {
        seedService.seedStudentsOnly();
        return ResponseEntity.ok(Map.of("status", "Students seeded"));
    }

    @PostMapping("/attendance")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Map<String, String>> seedAttendance() {
        seedService.seedAttendanceOnly();
        return ResponseEntity.ok(Map.of("status", "Attendance seeded"));
    }

    @PostMapping("/makeup")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Map<String, String>> seedMakeup() {
        seedService.seedMakeupOnly();
        return ResponseEntity.ok(Map.of("status", "Makeup requests seeded"));
    }
}
