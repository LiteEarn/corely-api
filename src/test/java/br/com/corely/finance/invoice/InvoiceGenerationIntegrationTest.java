package br.com.corely.finance.invoice;

import br.com.corely.billingconfiguration.BillingConfiguration;
import br.com.corely.billingconfiguration.BillingConfigurationRepository;
import br.com.corely.finance.invoice.dto.DashboardResponse;
import br.com.corely.finance.invoice.dto.GenerateInvoiceRequest;
import br.com.corely.finance.invoice.dto.GenerateInvoiceResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceGenerationIntegrationTest {

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
    private BillingConfigurationRepository billingConfigurationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvoiceGenerationService invoiceGenerationService;

    private Studio studio;
    private Student student1;
    private Student student2;
    private Student studentWithBillingDisabled;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Invoice Gen Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        student1 = createAndSaveStudent("Student One", true, true);
        student2 = createAndSaveStudent("Student Two", true, true);
        studentWithBillingDisabled = createAndSaveStudent("No Billing", true, false);

        createAndSaveStudent("Inactive", false, true);

        var config = new BillingConfiguration();
        config.setStudio(studio);
        config.setDueDay(15);
        config.setDefaultAmount(BigDecimal.valueOf(199));
        config.setActive(true);
        billingConfigurationRepository.save(config);
    }

    @Test
    void generate_shouldCreateInvoicesForEligibleStudents() throws Exception {
        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        mockMvc.perform(post("/finance/invoices/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.ignored").value(1))
                .andExpect(jsonPath("$.executionTimeMillis").isNumber());
    }

    @Test
    void generate_shouldNotCreateDuplicates() {
        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        var first = invoiceGenerationService.generate(request, studio.getId());
        assertThat(first.getCreated()).isEqualTo(2);
        assertThat(first.getIgnored()).isEqualTo(1);

        var second = invoiceGenerationService.generate(request, studio.getId());
        assertThat(second.getCreated()).isEqualTo(0);
        assertThat(second.getIgnored()).isEqualTo(3);
    }

    @Test
    void generate_shouldUseBillingConfigurationDueDay() throws Exception {
        var config = billingConfigurationRepository.findByStudioId(studio.getId()).orElseThrow();
        config.setDueDay(10);
        billingConfigurationRepository.save(config);

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-09");

        var response = invoiceGenerationService.generate(request, studio.getId());
        assertThat(response.getCreated()).isEqualTo(2);

        var invoices = invoiceRepository.findAll();
        var invoice = invoices.stream().filter(i -> i.getStudent().getId().equals(student1.getId())).findFirst().orElseThrow();
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    void dashboard_shouldReturnCorrectMetrics() throws Exception {
        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");
        invoiceGenerationService.generate(request, studio.getId());

        mockMvc.perform(get("/finance/invoices/dashboard")
                        .param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingInvoices").value(2))
                .andExpect(jsonPath("$.paidInvoices").value(0))
                .andExpect(jsonPath("$.overdueInvoices").value(0))
                .andExpect(jsonPath("$.cancelledInvoices").value(0))
                .andExpect(jsonPath("$.expectedRevenue").value(398))
                .andExpect(jsonPath("$.receivedRevenue").value(0))
                .andExpect(jsonPath("$.pendingRevenue").value(398))
                .andExpect(jsonPath("$.totalBilledStudents").value(2));
    }

    @Test
    void dashboard_shouldReflectPayments() {
        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");
        invoiceGenerationService.generate(request, studio.getId());

        var invoices = invoiceRepository.findAll();
        var invoice = invoices.get(0);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentDate(LocalDate.now());
        invoiceRepository.save(invoice);

        var dashboard = invoiceGenerationService.dashboard("2026-08", studio.getId());
        assertThat(dashboard.getPaidInvoices()).isEqualTo(1);
        assertThat(dashboard.getPendingInvoices()).isEqualTo(1);
        assertThat(dashboard.getReceivedRevenue()).isEqualByComparingTo(BigDecimal.valueOf(199));
        assertThat(dashboard.getPendingRevenue()).isEqualByComparingTo(BigDecimal.valueOf(199));
    }

    @Test
    void dashboard_shouldReturnEmptyDashboardWhenNoInvoices() throws Exception {
        mockMvc.perform(get("/finance/invoices/dashboard")
                        .param("month", "2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingInvoices").value(0))
                .andExpect(jsonPath("$.paidInvoices").value(0))
                .andExpect(jsonPath("$.overdueInvoices").value(0))
                .andExpect(jsonPath("$.cancelledInvoices").value(0))
                .andExpect(jsonPath("$.expectedRevenue").value(0))
                .andExpect(jsonPath("$.receivedRevenue").value(0))
                .andExpect(jsonPath("$.pendingRevenue").value(0))
                .andExpect(jsonPath("$.totalBilledStudents").value(0));
    }

    @Test
    void multiTenantIsolation_shouldOnlyGenerateForOwnStudio() {
        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        var resultA = invoiceGenerationService.generate(request, studio.getId());
        assertThat(resultA.getCreated()).isEqualTo(2);
        assertThat(resultA.getIgnored()).isEqualTo(1);

        createAndSaveStudent("Should Not Appear", false, true);

        var resultA2 = invoiceGenerationService.generate(request, studio.getId());
        assertThat(resultA2.getCreated()).isEqualTo(0);
        assertThat(resultA2.getIgnored()).isEqualTo(3);
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

    private Student createAndSaveStudent(String name, boolean active, boolean billingEnabled) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(active);
        student.setBillingEnabled(billingEnabled);
        return studentRepository.save(student);
    }
}
