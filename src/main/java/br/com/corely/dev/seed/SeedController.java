package br.com.corely.dev.seed;

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
    public ResponseEntity<SeedResponse> seedAll() {
        SeedResponse response = seedService.execute();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dashboard")
    public ResponseEntity<Map<String, String>> seedDashboard() {
        seedService.ensureDashboardData();
        return ResponseEntity.ok(Map.of("status", "Dashboard data adjusted"));
    }

    @PostMapping("/students")
    public ResponseEntity<Map<String, String>> seedStudents() {
        seedService.seedStudentsOnly();
        return ResponseEntity.ok(Map.of("status", "Students seeded"));
    }

    @PostMapping("/attendance")
    public ResponseEntity<Map<String, String>> seedAttendance() {
        seedService.seedAttendanceOnly();
        return ResponseEntity.ok(Map.of("status", "Attendance seeded"));
    }

    @PostMapping("/makeup")
    public ResponseEntity<Map<String, String>> seedMakeup() {
        seedService.seedMakeupOnly();
        return ResponseEntity.ok(Map.of("status", "Makeup requests seeded"));
    }
}
