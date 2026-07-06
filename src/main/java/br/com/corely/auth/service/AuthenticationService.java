package br.com.corely.auth.service;

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
import br.com.corely.auth.security.jwt.JwtService;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationFacade authenticationFacade;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

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
                .studioId(user.getStudio().getId())
                .studioName(user.getStudio().getName())
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
        return CurrentUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .studio(CurrentStudioResponse.builder()
                        .id(user.getStudio().getId())
                        .name(user.getStudio().getName())
                        .build())
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
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, newRefreshToken);

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
}
