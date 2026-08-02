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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private User adminUser;
    private User receptionistUser;
    private User instructorUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        studio = new Studio();
        studio.setName("Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);

        adminUser = createUser("Admin", UserRole.ADMIN);
        receptionistUser = createUser("Receptionist", UserRole.RECEPTIONIST);
        instructorUser = createUser("Instructor", UserRole.INSTRUCTOR);
        studentUser = createUser("Student", UserRole.STUDENT);
    }

    @Test
    void admin_shouldAccessDashboard() throws Exception {
        authenticateAs(adminUser);
        mockMvc.perform(get("/dashboard")
                        .param("studioId", studio.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void instructor_shouldNotAccessDashboard() throws Exception {
        authenticateAs(instructorUser);
        mockMvc.perform(get("/dashboard")
                        .param("studioId", studio.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_shouldAccessInstructors() throws Exception {
        authenticateAs(adminUser);
        mockMvc.perform(get("/instructors"))
                .andExpect(status().isOk());
    }

    @Test
    void receptionist_shouldNotAccessInstructors() throws Exception {
        authenticateAs(receptionistUser);
        mockMvc.perform(get("/instructors"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionist_shouldAccessStudents() throws Exception {
        authenticateAs(receptionistUser);
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk());
    }

    @Test
    void instructor_shouldAccessObjectives() throws Exception {
        authenticateAs(instructorUser);
        mockMvc.perform(get("/objectives"))
                .andExpect(status().isOk());
    }

    @Test
    void student_shouldNotAccessObjectives() throws Exception {
        authenticateAs(studentUser);
        mockMvc.perform(get("/objectives"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_shouldAccessClassGroups() throws Exception {
        authenticateAs(adminUser);
        mockMvc.perform(get("/class-groups"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().isForbidden());
    }

    private User createUser(String name, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(role.name().toLowerCase() + "_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        return userRepository.save(user);
    }

    private void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
