package br.com.corely.audit;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Endpoints de consulta da trilha de auditoria (EPIC-02-S09).
 *
 * <p>Acesso restrito a {@code OWNER} e {@code ADMIN}. A consulta é sempre
 * filtrada pelo estúdio corrente (multi-tenant via {@link AuditService}).</p>
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Trilha de auditoria LGPD (eventos de autenticação e segurança)")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Listar registros de auditoria",
            description = "Lista os registros de auditoria do estúdio corrente, com filtros opcionais por evento, usuário e intervalo de datas. Restrito a OWNER e ADMIN.")
    public ResponseEntity<Page<AuditLogResponse>> findAll(
            @Parameter(description = "Filtro por evento auditado") @RequestParam(required = false) AuditEvent event,
            @Parameter(description = "Filtro por usuário que executou a ação") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Data/hora inicial (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Data/hora final (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.findAll(event, userId, from, to, pageable));
    }
}
