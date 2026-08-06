package br.com.corely.finance.payment;

import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.receivable.ReceivableStatus;
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
 * Testes de integração do endpoint de pagamentos — baixa manual (EPIC-03-S06).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentControllerTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        receivableRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = studioRepository.save(createStudio("Payment Studio"));
        student = createAndSaveStudent(studio, "Payment Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldReturn201AndSettleReceivable() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));

        var request = new PaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(150));
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.CASH);

        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivableId").value(receivable.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("Payment Student"))
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));

        var settled = receivableRepository.findById(receivable.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(settled.getStatus()).isEqualTo(ReceivableStatus.PAID);
    }

    @Test
    void create_shouldReturn404WhenReceivableBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(100));

        var request = new PaymentRequest();
        request.setReceivableId(otherReceivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(100));
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.PIX);

        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409WhenReceivableAlreadyPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        receivable.setStatus(ReceivableStatus.PAID);
        receivableRepository.save(receivable);

        var request = new PaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(100));
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.PIX);

        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409WhenAmountDoesNotMatch() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(199.90));

        var request = new PaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(100));
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.PIX);

        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withReceptionistRole_shouldReturn403() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        var request = new PaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(100));
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.PIX);

        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_shouldReturnOnlyCurrentTenantPayments() throws Exception {
        createPaymentViaApi(createAndSaveReceivable(BigDecimal.valueOf(120)));

        Studio otherStudio = studioRepository.save(createStudio("Other Finance Studio"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));

        var request = new PaymentRequest();
        request.setReceivableId(otherReceivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(80));
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.CASH);
        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/finance/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].studentName").value("Payment Student"));
    }

    @Test
    void findById_shouldReturnPaymentOfCurrentTenant() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(120));
        var payment = createPaymentViaApi(receivable);

        mockMvc.perform(get("/finance/payments/{id}", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()));
    }

    @Test
    void findById_shouldReturn404WhenPaymentBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));

        var otherPayment = new Payment();
        otherPayment.setStudio(otherStudio);
        otherPayment.setReceivable(otherReceivable);
        otherPayment.setPaymentDate(LocalDate.now());
        otherPayment.setAmount(BigDecimal.valueOf(80));
        otherPayment.setPaymentMethod(PaymentMethod.CASH);
        otherPayment = paymentRepository.save(otherPayment);

        mockMvc.perform(get("/finance/payments/{id}", otherPayment.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldRecordPaymentMovement() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        createPaymentViaApi(receivable);

        mockMvc.perform(get("/finance/receivables/{id}/movements", receivable.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].movementType").value("PAYMENT"));
    }

    private Payment createPaymentViaApi(Receivable receivable) throws Exception {
        var request = new PaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(receivable.getAmount());
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.CASH);

        String body = mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return paymentRepository.findById(
                java.util.UUID.fromString(objectMapper.readTree(body).get("id").asText())).orElseThrow();
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

    private Receivable createAndSaveReceivable(BigDecimal amount) {
        return createAndSaveReceivable(student, amount);
    }

    private Receivable createAndSaveReceivable(Student student, BigDecimal amount) {
        var receivable = new Receivable();
        receivable.setStudio(student.getStudio());
        receivable.setStudent(student);
        receivable.setDescription("Mensalidade");
        receivable.setAmount(amount);
        receivable.setDueDate(LocalDate.now().plusDays(30));
        receivable.setStatus(ReceivableStatus.OPEN);
        return receivableRepository.save(receivable);
    }

    private void createAndAuthenticateUser(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail("payment_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}