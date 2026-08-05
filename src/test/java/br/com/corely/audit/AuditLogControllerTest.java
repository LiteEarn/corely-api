package br.com.corely.audit;

import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do endpoint de consulta de auditoria (EPIC-02-S09).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "correctPassword123";

    private Studio studio;
    private User adminUser;
    private User receptionistUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Audit Controller Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);

        adminUser = createUser("admin@test.com", UserRole.ADMIN);
        receptionistUser = createUser("receptionist@test.com", UserRole.RECEPTIONIST);
    }

    @Test
    void findAll_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_withNonAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/audit-logs")
                        .header("Authorization", "Bearer " + login(receptionistUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_withAdminRole_shouldReturn200AndLogs() throws Exception {
        mockMvc.perform(get("/audit-logs")
                        .header("Authorization", "Bearer " + login(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private User createUser(String email, UserRole role) {
        User user = new User();
        user.setName(email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        return userRepository.save(user);
    }

    private String login(User user) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(user.getEmail(), RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}