package br.com.corely.finance.invoice;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Studio studioA;
    private Studio studioB;
    private Student studentA;
    private Student studentB;
    private Invoice invoiceA;
    private Invoice invoiceB;

    @BeforeEach
    void setUp() {
        studioA = studioRepository.save(createStudio("Studio A"));
        studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN, "admin-a@test.com");

        studentA = createAndSaveStudent(studioA, "Student A");
        studentB = createAndSaveStudent(studioB, "Student B");

        invoiceA = createInvoice(studioA, studentA, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(100));
        invoiceB = createInvoice(studioB, studentB, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(200));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAll_shouldOnlyReturnOwnTenantData() throws Exception {
        mockMvc.perform(get("/finance/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentName").value("Student A"));
    }

    @Test
    void findById_shouldReturn404_whenInvoiceBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/finance/invoices/{id}", invoiceB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void pay_shouldReturn404_whenInvoiceBelongsToOtherTenant() throws Exception {
        mockMvc.perform(post("/finance/invoices/{id}/pay", invoiceB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_shouldReturn404_whenInvoiceBelongsToOtherTenant() throws Exception {
        mockMvc.perform(post("/finance/invoices/{id}/cancel", invoiceB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldCreateForAuthenticatedTenant() throws Exception {
        var request = new br.com.corely.finance.invoice.dto.InvoiceRequest();
        request.setStudentId(studentA.getId());
        request.setDueDate(LocalDate.of(2026, 9, 15));
        request.setAmount(BigDecimal.valueOf(300));

        mockMvc.perform(post("/finance/invoices")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentName").value("Student A"));
    }

    private User createAndAuthenticateUser(Studio studio, UserRole role, String email) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(email);
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

    private Invoice createInvoice(Studio studio, Student student, LocalDate dueDate, BigDecimal amount) {
        var invoice = new Invoice();
        invoice.setStudio(studio);
        invoice.setStudent(student);
        invoice.setDueDate(dueDate);
        invoice.setAmount(amount);
        invoice.setStatus(InvoiceStatus.PENDING);
        return invoiceRepository.save(invoice);
    }
}
