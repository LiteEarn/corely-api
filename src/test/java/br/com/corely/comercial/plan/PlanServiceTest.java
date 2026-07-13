package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlanServiceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Studio studio;
    private Plan existingPlan;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        var session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter("comercialTenantFilter") == null) {
            session.enableFilter("comercialTenantFilter")
                    .setParameter("studioId", studio.getId());
        }

        existingPlan = planRepository.save(createPlan("Basic Plan", BigDecimal.valueOf(100), 30));
    }

    @Test
    void create_shouldPersistPlan() {
        var request = new PlanRequest();
        request.setName("Premium Plan");
        request.setDescription("Premium plan description");
        request.setPrice(BigDecimal.valueOf(200));
        request.setDuration(60);

        var response = planService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Premium Plan");
        assertThat(response.getDescription()).isEqualTo("Premium plan description");
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.getDuration()).isEqualTo(60);
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenNameAlreadyExists() {
        var request = new PlanRequest();
        request.setName("Basic Plan");
        request.setPrice(BigDecimal.valueOf(150));
        request.setDuration(30);

        assertThatThrownBy(() -> planService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plan name already exists");
    }

    @Test
    void create_shouldSetDefaultActiveTrue_whenNotProvided() {
        var request = new PlanRequest();
        request.setName("New Plan");
        request.setPrice(BigDecimal.valueOf(50));
        request.setDuration(15);

        var response = planService.create(request);

        assertThat(response.getActive()).isTrue();
    }

    @Test
    void findById_shouldReturnPlan() {
        var response = planService.findById(existingPlan.getId());

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Basic Plan");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> planService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        var page = planService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAll_shouldFilterByName() {
        var page = planService.findAll("Basic", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Basic Plan");
    }

    @Test
    void findAll_shouldFilterByActive() {
        var page = planService.findAll(null, true, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findAll_shouldReturnEmpty_whenNoMatch() {
        var page = planService.findAll("Nonexistent", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void update_shouldModifyPlan() {
        var request = new PlanRequest();
        request.setName("Updated Plan");
        request.setPrice(BigDecimal.valueOf(150));
        request.setDuration(45);

        var response = planService.update(existingPlan.getId(), request);

        assertThat(response.getName()).isEqualTo("Updated Plan");
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(response.getDuration()).isEqualTo(45);
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    void update_shouldThrowException_whenNameAlreadyExists() {
        var another = planRepository.save(createPlan("Another Plan", BigDecimal.valueOf(80), 20));

        var request = new PlanRequest();
        request.setName("Another Plan");
        request.setPrice(BigDecimal.valueOf(200));
        request.setDuration(30);

        assertThatThrownBy(() -> planService.update(existingPlan.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plan name already exists");
    }

    @Test
    void update_shouldAllowSameNameForSamePlan() {
        var request = new PlanRequest();
        request.setName("Basic Plan");
        request.setPrice(BigDecimal.valueOf(200));
        request.setDuration(60);

        var response = planService.update(existingPlan.getId(), request);

        assertThat(response.getName()).isEqualTo("Basic Plan");
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new PlanRequest();
        request.setName("Nonexistent");
        request.setPrice(BigDecimal.TEN);
        request.setDuration(30);

        assertThatThrownBy(() -> planService.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activate_shouldSetActiveTrue() {
        planService.inactivate(existingPlan.getId());

        planService.activate(existingPlan.getId());

        var entity = planRepository.findById(existingPlan.getId()).orElseThrow();
        assertThat(entity.getActive()).isTrue();
        assertThat(entity.getVersion()).isEqualTo(3);
    }

    @Test
    void activate_shouldThrowException_whenAlreadyActive() {
        assertThatThrownBy(() -> planService.activate(existingPlan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Plan is already active");
    }

    @Test
    void activate_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> planService.activate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inactivate_shouldSetActiveFalse() {
        planService.inactivate(existingPlan.getId());

        var entity = planRepository.findById(existingPlan.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
        assertThat(entity.getVersion()).isEqualTo(2);
    }

    @Test
    void inactivate_shouldThrowException_whenAlreadyInactive() {
        planService.inactivate(existingPlan.getId());

        assertThatThrownBy(() -> planService.inactivate(existingPlan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Plan is already inactive");
    }

    @Test
    void inactivate_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> planService.inactivate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldIncrementVersionOnUpdate() {
        var request = new PlanRequest();
        request.setName("Version Test");
        request.setPrice(BigDecimal.valueOf(100));
        request.setDuration(30);

        var created = planService.create(request);
        assertThat(created.getVersion()).isEqualTo(1);

        request.setPrice(BigDecimal.valueOf(200));
        var updated = planService.update(created.getId(), request);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Plan createPlan(String name, BigDecimal price, Integer duration) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        return plan;
    }
}
