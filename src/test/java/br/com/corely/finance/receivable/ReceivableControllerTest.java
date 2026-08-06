package br.com.corely.finance.receivable;

import br.com.corely.finance.receivable.dto.ReceivableRequest;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do endpoint de recebíveis (EPIC-03-S01).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReceivableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReceivableRepository receivableRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        receivableRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = studioRepository.save(createStudio("Finance Studio"));
        student = createAndSaveStudent(studio, "Receivable Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldReturn201AndOpenReceivable() throws Exception {
        var request = new ReceivableRequest();
        request.setStudentId(student.getId());
        request.setDescription("Mensalidade Setembro");
        request.setAmount(BigDecimal.valueOf(199.90));
        request.setDueDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/finance/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.amount").value(199.90))
                .andExpect(jsonPath("$.dueDate").value("2026-09-10"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void findAll_shouldReturnOnlyCurrentTenantReceivables() throws Exception {
        createAndSaveReceivable(student, "Mensalidade A");

        Studio otherStudio = studioRepository.save(createStudio("Other Finance Studio"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        createAndSaveReceivable(otherStudent, "Mensalidade B");

        mockMvc.perform(get("/finance/receivables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Mensalidade A"))
                .andExpect(jsonPath("$.content[0].studentName").value("Receivable Student"));
    }

    @Test
    void findAll_shouldFilterByStatus() throws Exception {
        createAndSaveReceivable(student, "Open One");
        var paid = createAndSaveReceivable(student, "Paid One");
        paid.setStatus(ReceivableStatus.PAID);
        receivableRepository.save(paid);

        mockMvc.perform(get("/finance/receivables").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Open One"));
    }

    @Test
    void findById_shouldReturnReceivableOfCurrentTenant() throws Exception {
        var receivable = createAndSaveReceivable(student, "By Id");

        mockMvc.perform(get("/finance/receivables/{id}", receivable.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(receivable.getId().toString()));
    }

    @Test
    void findById_shouldReturn404WhenReceivableBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, "Other");

        mockMvc.perform(get("/finance/receivables/{id}", otherReceivable.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnSituationInResponse() throws Exception {
        createAndSaveReceivable(student, "Open Receivable");

        mockMvc.perform(get("/finance/receivables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].situation").value("OPEN"));
    }

    @Test
    void findAll_shouldFilterByOverdueSituation() throws Exception {
        createAndSaveReceivable(student, "Not Due Yet");
        createAndSaveReceivable(student, "Overdue", LocalDate.now().minusDays(5));

        mockMvc.perform(get("/finance/receivables").param("situation", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Overdue"))
                .andExpect(jsonPath("$.content[0].situation").value("OVERDUE"));
    }

    @Test
    void findAll_shouldFilterByOpenSituation() throws Exception {
        createAndSaveReceivable(student, "Not Due Yet");
        createAndSaveReceivable(student, "Overdue", LocalDate.now().minusDays(5));

        mockMvc.perform(get("/finance/receivables").param("situation", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Not Due Yet"));
    }

    @Test
    void findAll_shouldFilterByReversedSituation() throws Exception {
        var cancelled = createAndSaveReceivable(student, "Cancelled One");
        cancelled.setStatus(ReceivableStatus.CANCELLED);
        receivableRepository.save(cancelled);

        mockMvc.perform(get("/finance/receivables").param("situation", "REVERSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].situation").value("REVERSED"));
    }

    @Test
    void create_shouldReturn400WhenAmountIsNegative() throws Exception {
        var request = new ReceivableRequest();
        request.setStudentId(student.getId());
        request.setAmount(BigDecimal.valueOf(-10));
        request.setDueDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/finance/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenStudentIdMissing() throws Exception {
        var request = new ReceivableRequest();
        request.setAmount(BigDecimal.valueOf(100));
        request.setDueDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/finance/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenDueDateMissing() throws Exception {
        var request = new ReceivableRequest();
        request.setStudentId(student.getId());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/finance/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenDescriptionExceedsMaxLength() throws Exception {
        var request = new ReceivableRequest();
        request.setStudentId(student.getId());
        request.setDescription("x".repeat(501));
        request.setAmount(BigDecimal.valueOf(100));
        request.setDueDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/finance/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_withInvertedDateRange_shouldReturn400() throws Exception {
        mockMvc.perform(get("/finance/receivables")
                        .param("dueDateFrom", "2026-09-10")
                        .param("dueDateTo", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withReceptionistRole_shouldReturn403() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        var request = new ReceivableRequest();
        request.setStudentId(student.getId());
        request.setAmount(BigDecimal.valueOf(100));
        request.setDueDate(LocalDate.of(2026, 9, 10));

        mockMvc.perform(post("/finance/receivables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
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
        student.setBillingEnabled(true);
        return studentRepository.save(student);
    }

    private Receivable createAndSaveReceivable(Student student, String description) {
        return createAndSaveReceivable(student, description, LocalDate.now().plusDays(30));
    }

    private Receivable createAndSaveReceivable(Student student, String description, LocalDate dueDate) {
        var receivable = new Receivable();
        receivable.setStudio(student.getStudio());
        receivable.setStudent(student);
        receivable.setDescription(description);
        receivable.setAmount(BigDecimal.valueOf(100));
        receivable.setDueDate(dueDate);
        receivable.setStatus(ReceivableStatus.OPEN);
        return receivableRepository.save(receivable);
    }

    private void createAndAuthenticateUser(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail("finance_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
