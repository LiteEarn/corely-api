package br.com.corely.comercial.ruledefinition;

import br.com.corely.comercial.ruledefinition.dto.RuleDefinitionRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RuleDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RuleDefinition activeRule;
    private RuleDefinition inactiveRule;

    @BeforeEach
    void setUp() {
        var studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        activeRule = ruleDefinitionRepository.save(createRule("ACTIVE_RULE", "Active Rule", true));
        inactiveRule = ruleDefinitionRepository.save(createRule("INACTIVE_RULE", "Inactive Rule", false));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new RuleDefinitionRequest();
        request.setCode("NEW_RULE");
        request.setName("New Rule");
        request.setValueType(ValueType.BOOLEAN);
        request.setCategory(Category.ATTENDANCE);

        mockMvc.perform(post("/comercial/rule-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value("NEW_RULE"))
                .andExpect(jsonPath("$.name").value("New Rule"))
                .andExpect(jsonPath("$.valueType").value("BOOLEAN"))
                .andExpect(jsonPath("$.category").value("ATTENDANCE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn409_whenCodeDuplicated() throws Exception {
        var request = new RuleDefinitionRequest();
        request.setCode("ACTIVE_RULE");
        request.setName("Duplicate");
        request.setValueType(ValueType.STRING);
        request.setCategory(Category.GENERAL);

        mockMvc.perform(post("/comercial/rule-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        var request = new RuleDefinitionRequest();
        request.setName("No Code");

        mockMvc.perform(post("/comercial/rule-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_shouldReturnOnlyActiveByDefault() throws Exception {
        mockMvc.perform(get("/comercial/rule-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void findAll_withActiveParamTrue_shouldReturnOnlyActive() throws Exception {
        mockMvc.perform(get("/comercial/rule-definitions?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("ACTIVE_RULE"));
    }

    @Test
    void findAllAdmin_shouldReturnAllRules() throws Exception {
        mockMvc.perform(get("/comercial/rule-definitions/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void findById_shouldReturnRule() throws Exception {
        mockMvc.perform(get("/comercial/rule-definitions/{id}", activeRule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACTIVE_RULE"))
                .andExpect(jsonPath("$.name").value("Active Rule"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/rule-definitions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        var request = new RuleDefinitionRequest();
        request.setCode("UPDATED_CODE");
        request.setName("Updated Name");
        request.setValueType(ValueType.DECIMAL);
        request.setCategory(Category.BILLING);

        mockMvc.perform(put("/comercial/rule-definitions/{id}", activeRule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED_CODE"))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.valueType").value("DECIMAL"))
                .andExpect(jsonPath("$.category").value("BILLING"));
    }

    @Test
    void update_shouldReturn409_whenCodeDuplicated() throws Exception {
        var request = new RuleDefinitionRequest();
        request.setCode("INACTIVE_RULE");
        request.setName("Updated");
        request.setValueType(ValueType.STRING);
        request.setCategory(Category.GENERAL);

        mockMvc.perform(put("/comercial/rule-definitions/{id}", activeRule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void inactivate_shouldReturn204() throws Exception {
        mockMvc.perform(post("/comercial/rule-definitions/{id}/inactivate", activeRule.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void inactivate_shouldReturn409_whenAlreadyInactive() throws Exception {
        mockMvc.perform(post("/comercial/rule-definitions/{id}/inactivate", inactiveRule.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void activate_shouldReturn204() throws Exception {
        mockMvc.perform(post("/comercial/rule-definitions/{id}/activate", inactiveRule.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void activate_shouldReturn409_whenAlreadyActive() throws Exception {
        mockMvc.perform(post("/comercial/rule-definitions/{id}/activate", activeRule.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/comercial/rule-definitions"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeForbiddenForReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var request = new RuleDefinitionRequest();
        request.setCode("FORBIDDEN");
        request.setName("Forbidden");
        request.setValueType(ValueType.STRING);
        request.setCategory(Category.GENERAL);

        mockMvc.perform(post("/comercial/rule-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
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

    private RuleDefinition createRule(String code, String name, boolean active) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(name);
        rule.setValueType(ValueType.STRING);
        rule.setCategory(Category.GENERAL);
        rule.setActive(active);
        return rule;
    }
}
