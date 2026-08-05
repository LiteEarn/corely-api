package br.com.corely.audit;

import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço da trilha de auditoria (EPIC-02-S09).
 *
 * <p>Registra eventos relevantes (autenticação, autorização, operações
 * sensíveis) com quem, quando, de onde e o que foi feito — requisitos de
 * rastreabilidade LGPD. A consulta é sempre restrita ao estúdio corrente
 * (multi-tenant).</p>
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    /**
     * Registra um evento de auditoria para o estúdio do usuário autenticado.
     *
     * @param event        evento auditado
     * @param resourceType tipo de recurso afetado (pode ser nulo)
     * @param resourceId   identificador do recurso afetado (pode ser nulo)
     * @param details      detalhes adicionais (pode ser nulo)
     * @param ipAddress    IP de origem (pode ser nulo)
     */
    @Transactional
    public void record(AuditEvent event, String resourceType, String resourceId,
                       String details, String ipAddress) {
        UUID studioId = tenantContext.getCurrentStudioId();
        UUID userId = tenantContext.getCurrentUserId();
        record(event, studioId, userId, resourceType, resourceId, details, ipAddress);
    }

    /**
     * Registra um evento de auditoria com estúdio e usuário explícitos
     * (usado em fluxos pré-autenticação, ex.: tentativa de login).
     *
     * @param event        evento auditado
     * @param studioId     estúdio ao qual o evento pertence
     * @param userId       usuário que executou a ação (pode ser nulo)
     * @param resourceType tipo de recurso afetado (pode ser nulo)
     * @param resourceId   identificador do recurso afetado (pode ser nulo)
     * @param details      detalhes adicionais (pode ser nulo)
     * @param ipAddress    IP de origem (pode ser nulo)
     */
    @Transactional
    public void record(AuditEvent event, UUID studioId, UUID userId,
                       String resourceType, String resourceId, String details, String ipAddress) {
        if (studioId == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setStudio(studioRepository.getReferenceById(studioId));
        if (userId != null) {
            log.setUser(createUserReference(userId));
        }
        log.setAction(event);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        log.setOccurredAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    /**
     * Consulta os registros de auditoria do estúdio corrente com filtros.
     *
     * @param event    filtro por evento (opcional)
     * @param userId   filtro por usuário (opcional)
     * @param from     filtro por data inicial (opcional)
     * @param to       filtro por data final (opcional)
     * @param pageable paginação
     * @return página de registros do estúdio corrente
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(AuditEvent event, UUID userId,
                                          LocalDateTime from, LocalDateTime to, Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        return auditLogRepository.findByFilters(studioId, event, userId, from, to, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        User user = log.getUser();
        return new AuditLogResponse(
                log.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getEmail() : null,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getOccurredAt()
        );
    }

    private User createUserReference(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
