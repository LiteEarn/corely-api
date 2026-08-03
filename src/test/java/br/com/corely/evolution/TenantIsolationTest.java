package br.com.corely.evolution;

import br.com.corely.evolution.dto.EvolutionRequest;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EvolutionRepository evolutionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Evolution evolutionA;
    private Evolution evolutionB;

    @BeforeEach
    void setUp() {
        Studio studioA = studioRepository.save(createStudio("Studio A"));
        Studio studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.INSTRUCTOR);

        Student studentA = createAndSaveStudent(studioA, "Student A");
        Student studentB = createAndSaveStudent(studioB, "Student B");

        evolutionA = createAndSaveEvolution(studioA, studentA, "Evolution A");
        evolutionB = createAndSaveEvolution(studioB, studentB, "Evolution B");

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnEvolutionsFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/evolutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Evolution A"));
    }

    @Test
    void findById_shouldReturn200_whenEvolutionBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/evolutions/{id}", evolutionA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Evolution A"));
    }

    @Test
    void findById_shouldReturn404_whenEvolutionBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/evolutions/{id}", evolutionB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn404_whenEvolutionBelongsToOtherTenant() throws Exception {
        var request = new EvolutionRequest();
        request.setStudentId(evolutionB.getStudent().getId());
        request.setEvolutionDate(LocalDate.now());
        request.setTitle("Hacked Evolution");
        request.setDescription("Hacked description");
        request.setCreatedBy("hacker@test.com");

        mockMvc.perform(put("/evolutions/{id}", evolutionB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404_whenEvolutionBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/evolutions/{id}", evolutionB.getId()))
                .andExpect(status().isNotFound());
    }

    private User createAndAuthenticateUser(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail("admin_a@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user;
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Student createAndSaveStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private Evolution createAndSaveEvolution(Studio studio, Student student, String title) {
        var evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setEvolutionDate(LocalDate.now());
        evolution.setTitle(title);
        evolution.setDescription("Description");
        evolution.setCreatedBy("instructor@test.com");
        return evolutionRepository.save(evolution);
    }
}
