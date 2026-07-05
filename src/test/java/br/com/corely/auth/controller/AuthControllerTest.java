package br.com.corely.auth.controller;

import br.com.corely.auth.dto.LoginRequest;
import br.com.corely.auth.dto.LoginResponse;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private String rawPassword = "correctPassword123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        studioRepository.deleteAll();

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
    void login_withValidCredentials_shouldReturn200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.user.name").value(user.getName()))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andExpect(jsonPath("$.studioId").value(user.getStudio().getId().toString()))
                .andExpect(jsonPath("$.studioName").value(user.getStudio().getName()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()));
    }

    @Test
    void login_withInvalidPassword_shouldReturn401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("wrongPassword")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_withNonExistentUser_shouldReturn401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password(rawPassword)
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withInvalidEmail_shouldReturn400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email")
                .password(rawPassword)
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withEmptyPassword_shouldReturn400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(user.getEmail())
                .password("")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withValidToken_shouldReturn200() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", loginResponse.getRefreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void refresh_withInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", "invalid-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldReturn204() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", loginResponse.getRefreshToken()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", loginResponse.getRefreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void businessEndpoint_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_withValidToken_shouldReturnCurrentUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(user.getEmail())
                .password(rawPassword)
                .build();

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        LoginResponse response = objectMapper.readValue(loginResponse, LoginResponse.class);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + response.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.name").value(user.getName()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value(user.getRole().name()))
                .andExpect(jsonPath("$.studio.id").value(user.getStudio().getId().toString()))
                .andExpect(jsonPath("$.studio.name").value(user.getStudio().getName()))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.lastLogin").isNotEmpty());
    }

    @Test
    void me_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }


}
