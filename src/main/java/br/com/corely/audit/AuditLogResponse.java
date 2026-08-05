package br.com.corely.audit;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de um registro de auditoria (EPIC-02-S09).
 *
 * @param id          identificador do log
 * @param userId      usuário que executou a ação (pode ser nulo em eventos pré-auth)
 * @param userEmail   e-mail do usuário que executou a ação
 * @param action      evento auditado
 * @param resourceType tipo de recurso afetado
 * @param resourceId  identificador do recurso afetado
 * @param details     detalhes adicionais
 * @param ipAddress   endereço IP de origem
 * @param occurredAt  data/hora do evento
 */
@Schema(description = "Registro da trilha de auditoria")
public record AuditLogResponse(
        @Schema(description = "Identificador do log") UUID id,
        @Schema(description = "Usuário que executou a ação (nulo em eventos pré-auth)", nullable = true) UUID userId,
        @Schema(description = "E-mail do usuário que executou a ação", nullable = true) String userEmail,
        @Schema(description = "Evento auditado") AuditEvent action,
        @Schema(description = "Tipo de recurso afetado", nullable = true) String resourceType,
        @Schema(description = "Identificador do recurso afetado", nullable = true) String resourceId,
        @Schema(description = "Detalhes adicionais", nullable = true) String details,
        @Schema(description = "Endereço IP de origem", nullable = true) String ipAddress,
        @Schema(description = "Data/hora do evento") LocalDateTime occurredAt) {
}
