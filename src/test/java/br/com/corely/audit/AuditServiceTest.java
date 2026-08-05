package br.com.corely.audit;

import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração do serviço de auditoria (EPIC-02-S09).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    private Studio studio;
    private User user;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Audit Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);

        user = new User();
        user.setName("Audit User");
        user.setEmail("audit@test.com");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);
    }

    @Test
    void record_withAuthenticatedUser_shouldPersistAuditLog() {
        authenticateAs(user);

        auditService.record(AuditEvent.LOGIN_SUCCESS, "AUTH", "LOGIN", "audit@test.com", "10.0.0.1");

        var logs = auditLogRepository.findByFilters(studio.getId(), null, null, null, null, PageRequest.of(0, 10));
        assertThat(logs.getContent()).hasSize(1);
        AuditLog log = logs.getContent().get(0);
        assertThat(log.getAction()).isEqualTo(AuditEvent.LOGIN_SUCCESS);
        assertThat(log.getUser()).isNotNull();
        assertThat(log.getUser().getId()).isEqualTo(user.getId());
        assertThat(log.getStudio().getId()).isEqualTo(studio.getId());
        assertThat(log.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(log.getOccurredAt()).isNotNull();
    }

    @Test
    void record_withExplicitStudioAndUser_shouldPersistEvenWithoutAuthContext() {
        auditService.record(AuditEvent.LOGIN_FAILED, studio.getId(), user.getId(),
                "AUTH", "LOGIN", "audit@test.com", "10.0.0.2");

        var logs = auditLogRepository.findByFilters(studio.getId(), AuditEvent.LOGIN_FAILED, null, null, null, PageRequest.of(0, 10));
        assertThat(logs.getContent()).hasSize(1);
        assertThat(logs.getContent().get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void record_withNullStudioId_shouldNotPersist() {
        auditService.record(AuditEvent.LOGIN_FAILED, null, null,
                "AUTH", "LOGIN", "ghost@test.com", "10.0.0.3");

        long count = auditLogRepository.count();
        assertThat(count).isZero();
    }

    @Test
    void findAll_shouldReturnOnlyCurrentTenantLogs() {
        authenticateAs(user);
        auditService.record(AuditEvent.LOGIN_SUCCESS, "AUTH", "LOGIN", "audit@test.com", "10.0.0.1");

        Studio otherStudio = new Studio();
        otherStudio.setName("Other Studio");
        otherStudio.setActive(true);
        otherStudio = studioRepository.save(otherStudio);

        auditService.record(AuditEvent.LOGIN_SUCCESS, otherStudio.getId(), null,
                "AUTH", "LOGIN", "other@test.com", "10.0.0.4");

        var page = auditService.findAll(null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).userId()).isEqualTo(user.getId());
    }

    @Test
    void findAll_shouldFilterByEventAndDateRange() {
        authenticateAs(user);
        auditService.record(AuditEvent.LOGIN_SUCCESS, "AUTH", "LOGIN", "audit@test.com", "10.0.0.1");
        auditService.record(AuditEvent.LOGOUT, "AUTH", "LOGOUT", "audit@test.com", "10.0.0.1");

        var logins = auditService.findAll(AuditEvent.LOGIN_SUCCESS, null, null, null, PageRequest.of(0, 10));
        assertThat(logins.getContent()).hasSize(1);
        assertThat(logins.getContent().get(0).action()).isEqualTo(AuditEvent.LOGIN_SUCCESS);

        var inFuture = auditService.findAll(null, null,
                LocalDateTime.now().plusDays(1), null, PageRequest.of(0, 10));
        assertThat(inFuture.getContent()).isEmpty();
    }

    @Test
    void findAll_shouldFilterByUserId() {
        authenticateAs(user);
        auditService.record(AuditEvent.LOGIN_SUCCESS, "AUTH", "LOGIN", "audit@test.com", "10.0.0.1");

        var byUser = auditService.findAll(null, user.getId(), null, null, PageRequest.of(0, 10));
        assertThat(byUser.getContent()).hasSize(1);

        var byOtherUser = auditService.findAll(null, UUID.randomUUID(), null, null, PageRequest.of(0, 10));
        assertThat(byOtherUser.getContent()).isEmpty();
    }

    private void authenticateAs(User principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}