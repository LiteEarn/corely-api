package br.com.corely.comercial.delinquencypolicy;

import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyActionDto;
import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyPolicyRequest;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DelinquencyPolicyIntegrationTest {

    @Autowired
    private DelinquencyPolicyService delinquencyPolicyService;

    @Autowired
    private DelinquencyPolicyRepository delinquencyPolicyRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Delinquency Studio"));
        authenticateAs(studio, UserRole.OWNER);
    }

    @Test
    void getOrCreate_shouldAutoCreatePolicy() {
        var response = delinquencyPolicyService.getOrCreate();

        assertThat(response.getId()).isNotNull();
        assertThat(response.getGracePeriodDays()).isEqualTo(0);
        assertThat(response.getAction()).isEqualTo(DelinquencyActionDto.NONE);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void getOrCreate_shouldReturnExistingPolicy() {
        var first = delinquencyPolicyService.getOrCreate();
        var second = delinquencyPolicyService.getOrCreate();

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void update_shouldChangePolicy() {
        delinquencyPolicyService.getOrCreate();

        var request = new DelinquencyPolicyRequest();
        request.setGracePeriodDays(10);
        request.setAction(DelinquencyActionDto.SUSPEND_CONTRACT);
        request.setActive(true);

        var response = delinquencyPolicyService.update(request);

        assertThat(response.getGracePeriodDays()).isEqualTo(10);
        assertThat(response.getAction()).isEqualTo(DelinquencyActionDto.SUSPEND_CONTRACT);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void update_shouldThrowException_whenNoPolicyExists() {
        var request = new DelinquencyPolicyRequest();
        request.setGracePeriodDays(5);
        request.setAction(DelinquencyActionDto.NONE);

        assertThatThrownBy(() -> delinquencyPolicyService.update(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Delinquency policy not found");
    }

    @Test
    void shouldHaveOnlyOnePolicyPerStudio() {
        var first = delinquencyPolicyService.getOrCreate();
        var second = delinquencyPolicyService.getOrCreate();

        assertThat(second.getId()).isEqualTo(first.getId());

        var all = delinquencyPolicyRepository.findAll();
        assertThat(all).hasSize(1);
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
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
}
