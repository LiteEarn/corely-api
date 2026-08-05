package br.com.corely.auth.service;

import br.com.corely.audit.AuditEvent;
import br.com.corely.audit.AuditService;
import br.com.corely.auth.authorization.RolePermissions;
import br.com.corely.auth.dto.CurrentStudioResponse;
import br.com.corely.auth.dto.CurrentUserResponse;
import br.com.corely.auth.dto.LoginRequest;
import br.com.corely.auth.dto.LoginResponse;
import br.com.corely.auth.dto.RefreshTokenRequest;
import br.com.corely.auth.dto.RefreshTokenResponse;
import br.com.corely.auth.entity.RefreshToken;
import br.com.corely.auth.repository.RefreshTokenRepository;
import br.com.corely.auth.security.AuthenticationFacade;
import br.com.corely.auth.security.ClientIpResolver;
import br.com.corely.auth.security.jwt.JwtService;
import br.com.corely.auth.security.lockout.LoginAttemptTracker;
import br.com.corely.auth.security.lockout.LoginLockoutException;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationFacade authenticationFacade;
    private final LoginAttemptTracker loginAttemptTracker;
    private final AuditService auditService;
    private final ClientIpResolver clientIpResolver;

    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String ip = clientIpResolver.resolveCurrentRequestIp();

        if (loginAttemptTracker.isLocked(email)) {
            int retryAfter = Math.max(1, loginAttemptTracker.getRemainingLockoutSeconds(email));
            throw new LoginLockoutException(
                    "Too many failed login attempts. Try again later.", retryAfter);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            var user = userRepository.findByEmail(email);
            user.ifPresent(u -> {
                UUID studioId = u.getStudio() != null ? u.getStudio().getId() : null;
                auditSafely(AuditEvent.LOGIN_FAILED, studioId, u.getId(), "AUTH", "LOGIN", email, ip);
            });
            boolean locked = loginAttemptTracker.recordFailure(email);
            if (locked) {
                user.ifPresent(u -> {
                    UUID studioId = u.getStudio() != null ? u.getStudio().getId() : null;
                    auditSafely(AuditEvent.LOCKOUT_TRIGGERED, studioId, u.getId(), "AUTH", "LOGIN", email, ip);
                });
                int retryAfter = Math.max(1, loginAttemptTracker.getRemainingLockoutSeconds(email));
                throw new LoginLockoutException(
                        "Too many failed login attempts. Try again later.", retryAfter);
            }
            throw e;
        }

        loginAttemptTracker.reset(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        auditSafely(AuditEvent.LOGIN_SUCCESS,
                user.getStudio() != null ? user.getStudio().getId() : null,
                user.getId(), "AUTH", "LOGIN", email, ip);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);

        List<String> permissions = RolePermissions.getPermissions(user.getRole()).stream()
                .map(Enum::name)
                .toList();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(buildCurrentUserResponse(user))
                .studioId(user.getStudio() != null ? user.getStudio().getId() : null)
                .studioName(user.getStudio() != null ? user.getStudio().getName() : null)
                .role(user.getRole().name())
                .permissions(permissions)
                .build();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me() {
        User user = authenticationFacade.getCurrentUser();
        if (user == null) {
            throw new BadCredentialsException("User not authenticated");
        }
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return buildCurrentUserResponse(freshUser);
    }

    private CurrentUserResponse buildCurrentUserResponse(User user) {
        List<String> permissions = RolePermissions.getPermissions(user.getRole()).stream()
                .map(Enum::name)
                .toList();
        CurrentStudioResponse studioResponse = user.getStudio() != null
                ? CurrentStudioResponse.builder()
                        .id(user.getStudio().getId())
                        .name(user.getStudio().getName())
                        .build()
                : null;
        return CurrentUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .studio(studioResponse)
                .permissions(permissions)
                .lastLogin(user.getLastLogin())
                .build();
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        if (user == null) {
            throw new BadCredentialsException("Invalid refresh token: user not found");
        }
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, newRefreshToken);

        auditSafely(AuditEvent.TOKEN_REFRESH,
                user.getStudio() != null ? user.getStudio().getId() : null,
                user.getId(), "AUTH", "REFRESH", user.getEmail(),
                clientIpResolver.resolveCurrentRequestIp());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            User user = token.getUser();
            if (user != null) {
                auditSafely(AuditEvent.LOGOUT,
                        user.getStudio() != null ? user.getStudio().getId() : null,
                        user.getId(), "AUTH", "LOGOUT", user.getEmail(),
                        clientIpResolver.resolveCurrentRequestIp());
            }
        });
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Registra um evento de auditoria de forma <b>fail-open</b>: uma falha na
     * auditoria (ex.: indisponibilidade do banco) nunca deve quebrar o contrato
     * de autenticação (200/401/429). O erro é logado e o fluxo segue.
     */
    private void auditSafely(AuditEvent event, UUID studioId, UUID userId,
                             String resourceType, String resourceId, String details, String ipAddress) {
        try {
            auditService.record(event, studioId, userId, resourceType, resourceId, details, ipAddress);
        } catch (RuntimeException ex) {
            log.warn("Falha ao registrar auditoria para o evento {} (resource={}): {}",
                    event, resourceId, ex.getMessage());
        }
    }
}
