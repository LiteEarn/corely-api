package br.com.corely.auth.security.jwt;

import br.com.corely.studio.Studio;
import br.com.corely.user.User;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);

        Studio studio = new Studio();
        studio.setId(UUID.randomUUID());
        studio.setName("Test Studio");
        user.setStudio(studio);
    }

    @Test
    void generateAccessToken_shouldContainAllClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractStudioId(token)).isEqualTo(user.getStudio().getId());
        assertThat(jwtService.extractRole(token)).isEqualTo(user.getRole().name());
        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.extractName(token)).isEqualTo(user.getName());
    }

    @Test
    void generateRefreshToken_shouldBeValid() {
        String token = jwtService.generateRefreshToken(user);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withValidToken_shouldReturnTrue() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token, user.getEmail())).isTrue();
    }

    @Test
    void isTokenValid_withInvalidUsername_shouldReturnFalse() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token, "wrong@email.com")).isFalse();
    }

    @Test
    void isTokenValid_withoutUsernameCheck_shouldReturnTrueForValidToken() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withInvalidToken_shouldReturnFalse() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }

    @Test
    void getAccessTokenExpiration_shouldReturnPositiveValue() {
        assertThat(jwtService.getAccessTokenExpiration()).isPositive();
    }

    @Test
    void getRefreshTokenExpiration_shouldReturnPositiveValue() {
        assertThat(jwtService.getRefreshTokenExpiration()).isPositive();
    }
}
