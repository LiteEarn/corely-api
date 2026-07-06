package br.com.corely.dev.seed;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev/seed")
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
