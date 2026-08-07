package br.com.corely.finance.refund;

import br.com.corely.finance.movement.ReceivableMovementRepository;
import br.com.corely.finance.payment.Payment;
import br.com.corely.finance.payment.PaymentMethod;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.receivable.ReceivableStatus;
import br.com.corely.finance.refund.dto.RefundRequest;
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
 * Testes de integração do endpoint de estorno de pagamentos (EPIC-03-S10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RefundControllerTest {

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
    private ReceivableMovementRepository movementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        movementRepository.deleteAll();
        receivableRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = studioRepository.save(createStudio("Refund Studio"));
        student = createAndSaveStudent(studio, "Refund Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldRefundAndReopenReceivable() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var payment = createPaymentViaApi(receivable);
        receivable.setStatus(ReceivableStatus.PAID);
        receivableRepository.save(receivable);

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());
        request.setReason("Cliente desistiu");

        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivableId").value(receivable.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("Refund Student"))
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.reason").value("Cliente desistiu"))
                .andExpect(jsonPath("$.refundedAt").isNotEmpty());

        var reopened = receivableRepository.findById(receivable.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reopened.getStatus()).isEqualTo(ReceivableStatus.OPEN);
    }

    @Test
    void create_shouldReturn404WhenPaymentBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(100));
        var otherPayment = createPaymentDirectly(otherReceivable, otherStudio);

        var request = new RefundRequest();
        request.setPaymentId(otherPayment.getId());

        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409WhenAlreadyRefunded() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var payment = createPaymentViaApi(receivable);
        refundViaApi(payment.getId());

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());

        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400WhenPaymentIdMissing() throws Exception {
        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenReasonExceeds500Chars() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        var payment = createPaymentDirectly(receivable, studio);

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());
        request.setReason("R".repeat(501));

        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withReceptionistRole_shouldReturn403() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        var payment = createPaymentDirectly(receivable, studio);

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());

        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldRecordRefundMovement() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var payment = createPaymentViaApi(receivable);
        refundViaApi(payment.getId());

        mockMvc.perform(get("/finance/receivables/{id}/movements", receivable.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].movementType").value("REFUND"));
    }

    @Test
    void findAll_shouldReturnOnlyRefundedPayments() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var payment = createPaymentViaApi(receivable);
        refundViaApi(payment.getId());

        // Pagamento não estornado não deve aparecer.
        var secondReceivable = createAndSaveReceivable(BigDecimal.valueOf(80));
        createPaymentDirectly(secondReceivable, studio);

        mockMvc.perform(get("/finance/refunds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].studentName").value("Refund Student"));
    }

    @Test
    void findAll_withReceptionistRole_shouldReturn200() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var payment = createPaymentViaApi(receivable);
        refundViaApi(payment.getId());

        // Troca para RECEPTIONIST apenas para a leitura.
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/finance/refunds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private Payment createPaymentViaApi(Receivable receivable) throws Exception {
        var request = new br.com.corely.finance.payment.dto.PaymentRequest();
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
                UUID.fromString(objectMapper.readTree(body).get("id").asText())).orElseThrow();
    }

    private void refundViaApi(UUID paymentId) throws Exception {
        var request = new RefundRequest();
        request.setPaymentId(paymentId);

        mockMvc.perform(post("/finance/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private Payment createPaymentDirectly(Receivable receivable, Studio studio) {
        var payment = new Payment();
        payment.setStudio(studio);
        payment.setReceivable(receivable);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(receivable.getAmount());
        payment.setPaymentMethod(PaymentMethod.CASH);
        return paymentRepository.save(payment);
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
        user.setEmail("refund_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
