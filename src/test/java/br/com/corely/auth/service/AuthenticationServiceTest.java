package br.com.corely.auth.service;

import br.com.corely.audit.AuditLogRepository;
import br.com.corely.auth.dto.LoginRequest;
import br.com.corely.auth.dto.LoginResponse;
import br.com.corely.auth.dto.RefreshTokenRequest;
import br.com.corely.auth.dto.RefreshTokenResponse;
import br.com.corely.auth.entity.RefreshToken;
import br.com.corely.auth.repository.RefreshTokenRepository;
import br.com.corely.auth.security.lockout.LoginAttemptTracker;
import br.com.corely.auth.security.lockout.LoginLockoutException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthenticationServiceTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptTracker loginAttemptTracker;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User user;
    private String rawPassword = "correctPassword123";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();
        loginAttemptTracker.reset("test@test.com");
        loginAttemptTracker.reset("nonexistent@test.com");

        Studio studio = new Studio();
        studio.setName("Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);

        user = new User();
        user.setName("Test User");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);
    }

    @Test
    void login_withValidCredentials_shouldReturnTokens() {
        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        LoginResponse response = authenticationService.login(request);

        assertThat(response.getAccessToken()).isNotNull();
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getExpiresIn()).isPositive();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(user.getId());
        assertThat(response.getUser().getName()).isEqualTo(user.getName());
        assertThat(response.getUser().getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getStudioId()).isEqualTo(user.getStudio().getId());
        assertThat(response.getStudioName()).isEqualTo(user.getStudio().getName());
        assertThat(response.getRole()).isEqualTo(user.getRole().name());
    }

    @Test
    void login_withInvalidPassword_shouldThrowException() {
        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("wrongPassword")
                .build();

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withNonExistentUser_shouldThrowException() {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password(rawPassword)
                .build();

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_withValidToken_shouldReturnNewTokens() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();

        RefreshTokenResponse refreshResponse = authenticationService.refresh(refreshRequest);

        assertThat(refreshResponse.getAccessToken()).isNotNull();
        assertThat(refreshResponse.getRefreshToken()).isNotNull();
        assertThat(refreshResponse.getExpiresIn()).isPositive();
    }

    @Test
    void refresh_withInvalidToken_shouldThrowException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-refresh-token")
                .build();

        assertThatThrownBy(() -> authenticationService.refresh(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_shouldRotateToken() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();
        RefreshTokenResponse refreshResponse = authenticationService.refresh(refreshRequest);

        assertThat(refreshResponse.getAccessToken()).isNotNull();
        assertThat(refreshResponse.getRefreshToken()).isNotNull();
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(loginResponse.getRefreshToken());
    }

    @Test
    void refresh_withRevokedToken_shouldThrowException() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        authenticationService.logout(loginResponse.getRefreshToken());

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();
        assertThatThrownBy(() -> authenticationService.refresh(refreshRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_shouldRevokeToken() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        authenticationService.logout(loginResponse.getRefreshToken());

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();
        assertThatThrownBy(() -> authenticationService.refresh(refreshRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_withInvalidToken_shouldNotThrowException() {
        authenticationService.logout("invalid-token");
    }

    @Test
    void me_whenAuthenticated_shouldReturnCurrentUser() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        authenticationService.login(loginRequest);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        var response = authenticationService.me();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(user.getId());
        assertThat(response.getName()).isEqualTo(user.getName());
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getRole()).isEqualTo(user.getRole().name());
        assertThat(response.getStudio()).isNotNull();
        assertThat(response.getStudio().getId()).isEqualTo(user.getStudio().getId());
        assertThat(response.getStudio().getName()).isEqualTo(user.getStudio().getName());
        assertThat(response.getPermissions()).isNotNull();
        assertThat(response.getLastLogin()).isNotNull();

        SecurityContextHolder.clearContext();
    }

    @Test
    void me_whenNotAuthenticated_shouldThrowException() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> authenticationService.me())
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_afterMaxFailedAttempts_shouldBeLocked() {
        LoginRequest badRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password("wrongPassword")
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authenticationService.login(badRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // A 5ª tentativa inválida dispara o lockout.
        assertThatThrownBy(() -> authenticationService.login(badRequest))
                .isInstanceOf(LoginLockoutException.class)
                .hasMessageContaining("Too many failed login attempts");

        // Tentativas seguintes permanecem bloqueadas.
        assertThatThrownBy(() -> authenticationService.login(badRequest))
                .isInstanceOf(LoginLockoutException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void login_withValidCredentials_shouldResetFailures() {
        LoginRequest badRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password("wrongPassword")
                .build();
        LoginRequest goodRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authenticationService.login(badRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // Login bem-sucedido reseta as tentativas.
        LoginResponse response = authenticationService.login(goodRequest);
        assertThat(response.getAccessToken()).isNotNull();

        // Nova falha não dispara lockout (contagem recomeçou).
        assertThatThrownBy(() -> authenticationService.login(badRequest))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(loginAttemptTracker.isLocked(user.getEmail())).isFalse();
    }

    @Test
    void login_shouldAuditSuccessAndFailureEvents() {
        LoginRequest badRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password("wrongPassword")
                .build();
        LoginRequest goodRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        assertThatThrownBy(() -> authenticationService.login(badRequest))
                .isInstanceOf(BadCredentialsException.class);

        authenticationService.login(goodRequest);

        var events = auditLogRepository.findAll()
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(events)
                .as("login deve auditar LOGIN_FAILED e LOGIN_SUCCESS")
                .contains(
                        br.com.corely.audit.AuditEvent.LOGIN_FAILED,
                        br.com.corely.audit.AuditEvent.LOGIN_SUCCESS
                );
    }

    @Test
    void logout_shouldAuditEvent() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        authenticationService.logout(loginResponse.getRefreshToken());

        var events = auditLogRepository.findAll()
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(events)
                .as("logout deve auditar LOGOUT")
                .contains(br.com.corely.audit.AuditEvent.LOGOUT);
    }

    @Test
    void refresh_shouldAuditEvent() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();
        LoginResponse loginResponse = authenticationService.login(loginRequest);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();
        authenticationService.refresh(refreshRequest);

        var events = auditLogRepository.findAll()
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(events)
                .as("refresh de token deve auditar TOKEN_REFRESH")
                .contains(br.com.corely.audit.AuditEvent.TOKEN_REFRESH);
    }

    @Test
    void login_reachingLockout_shouldAuditLockoutTriggered() {
        LoginRequest badRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password("wrongPassword")
                .build();

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authenticationService.login(badRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // A 5ª falha dispara o lockout e deve auditar LOCKOUT_TRIGGERED.
        assertThatThrownBy(() -> authenticationService.login(badRequest))
                .isInstanceOf(LoginLockoutException.class);

        var events = auditLogRepository.findAll()
                .stream()
                .map(log -> log.getAction())
                .toList();

        assertThat(events)
                .as("lockout deve auditar LOCKOUT_TRIGGERED e LOGIN_FAILED")
                .contains(br.com.corely.audit.AuditEvent.LOCKOUT_TRIGGERED)
                .contains(br.com.corely.audit.AuditEvent.LOGIN_FAILED);
    }
}
