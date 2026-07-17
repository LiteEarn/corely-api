package br.com.corely.finance.invoice;

import br.com.corely.finance.invoice.dto.InvoiceRequest;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceControllerTest {

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

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Finance Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        student = createAndSaveStudent("John Doe");
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new InvoiceRequest();
        request.setStudentId(student.getId());
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(150));
        request.setDescription("Mensalidade Agosto");

        mockMvc.perform(post("/finance/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("John Doe"))
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void create_shouldReturn404_whenStudentNotFound() throws Exception {
        var request = new InvoiceRequest();
        request.setStudentId(UUID.randomUUID());
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(150));

        mockMvc.perform(post("/finance/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnAllInvoices() throws Exception {
        createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));

        mockMvc.perform(get("/finance/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_shouldReturnInvoice() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));

        mockMvc.perform(get("/finance/invoices/{id}", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoice.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/finance/invoices/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void pay_shouldReturnOk() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));

        mockMvc.perform(post("/finance/invoices/{id}/pay", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentDate").isNotEmpty());
    }

    @Test
    void pay_shouldReturn409_whenAlreadyPaid() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));
        markAsPaid(invoice);

        mockMvc.perform(post("/finance/invoices/{id}/pay", invoice.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invoice is already paid"));
    }

    @Test
    void pay_shouldReturn409_whenCancelled() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));
        cancelInvoice(invoice);

        mockMvc.perform(post("/finance/invoices/{id}/pay", invoice.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot pay a cancelled invoice"));
    }

    @Test
    void cancel_shouldReturnOk() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));

        mockMvc.perform(post("/finance/invoices/{id}/cancel", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_shouldReturn409_whenPaid() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));
        markAsPaid(invoice);

        mockMvc.perform(post("/finance/invoices/{id}/cancel", invoice.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot cancel a paid invoice"));
    }

    @Test
    void cancel_shouldReturnOk_whenAlreadyCancelled() throws Exception {
        var invoice = createInvoice(student, LocalDate.of(2026, 8, 15), BigDecimal.valueOf(150));
        cancelInvoice(invoice);

        mockMvc.perform(post("/finance/invoices/{id}/cancel", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(post("/finance/invoices/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void pay_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(post("/finance/invoices/{id}/pay", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
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

    private Student createAndSaveStudent(String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private Invoice createInvoice(Student student, LocalDate dueDate, BigDecimal amount) {
        var invoice = new Invoice();
        invoice.setStudio(studio);
        invoice.setStudent(student);
        invoice.setDueDate(dueDate);
        invoice.setAmount(amount);
        invoice.setStatus(InvoiceStatus.PENDING);
        return invoiceRepository.save(invoice);
    }

    private void markAsPaid(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentDate(LocalDate.now());
        invoiceRepository.save(invoice);
    }

    private void cancelInvoice(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
    }
}
