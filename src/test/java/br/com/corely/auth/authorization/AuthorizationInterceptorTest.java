package br.com.corely.auth.authorization;

import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthorizationInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;

    @BeforeEach
    void setUp() {
        studio = new Studio();
        studio.setName("Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_shouldAccessDashboard() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .param("studioId", studio.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructor_shouldNotAccessDashboard() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .param("studioId", studio.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_shouldAccessInstructors() throws Exception {
        mockMvc.perform(get("/instructors"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionist_shouldNotAccessInstructors() throws Exception {
        mockMvc.perform(get("/instructors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionist_shouldAccessStudents() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructor_shouldAccessObjectives() throws Exception {
        mockMvc.perform(get("/objectives"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void student_shouldNotAccessObjectives() throws Exception {
        mockMvc.perform(get("/objectives"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_shouldAccessClassGroups() throws Exception {
        mockMvc.perform(get("/class-groups"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().isForbidden());
    }
}
