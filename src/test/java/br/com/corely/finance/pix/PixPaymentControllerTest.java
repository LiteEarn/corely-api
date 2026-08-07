package br.com.corely.finance.pix;

import br.com.corely.finance.pix.dto.PixPaymentRequest;
import br.com.corely.finance.movement.ReceivableMovementRepository;
import br.com.corely.finance.payment.PaymentRepository;
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
import org.springframework.test.annotation.Commit;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do endpoint de cobranças Pix (EPIC-03-S07).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PixPaymentControllerTest {

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
    private PixPaymentRepository pixPaymentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReceivableMovementRepository movementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        // Limpeza em transação própria (commitada) para remover dados persistidos
        // por testes anotados com @Commit, que não são revertidos pelo rollback
        // padrão da transação de teste.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            pixPaymentRepository.deleteAll();
            paymentRepository.deleteAll();
            movementRepository.deleteAll();
            receivableRepository.deleteAll();
            studentRepository.deleteAll();
            userRepository.deleteAll();
            studioRepository.deleteAll();
        });

        studio = studioRepository.save(createStudio("Pix Studio"));
        student = createAndSaveStudent(studio, "Pix Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldReturn201WithTxidAndCopyPaste() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));

        var request = new PixPaymentRequest();
        request.setReceivableId(receivable.getId());

        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivableId").value(receivable.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("Pix Student"))
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.txid").isNotEmpty())
                .andExpect(jsonPath("$.copyPaste").isNotEmpty());
    }

    @Test
    void create_shouldReturn404WhenReceivableBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(100));

        var request = new PixPaymentRequest();
        request.setReceivableId(otherReceivable.getId());

        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409WhenReceivableAlreadyPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        receivable.setStatus(ReceivableStatus.PAID);
        receivableRepository.save(receivable);

        var request = new PixPaymentRequest();
        request.setReceivableId(receivable.getId());

        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409WhenReceivableAlreadyHasPixCharge() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        createPixViaApi(receivable);

        var request = new PixPaymentRequest();
        request.setReceivableId(receivable.getId());

        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldEnforceUniqueReceivableConstraint() {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        createPixDirectly(receivable);

        var second = new PixPayment();
        second.setStudio(studio);
        second.setReceivable(receivable);
        second.setTxid("SECONDTXID" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        second.setCopyPaste("000201...");
        second.setAmount(BigDecimal.valueOf(100));
        second.setStatus(PixPaymentStatus.PENDING);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pixPaymentRepository.saveAndFlush(second))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void create_shouldReturn400WhenReceivableMissing() throws Exception {
        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withReceptionistRole_shouldReturn403() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        var request = new PixPaymentRequest();
        request.setReceivableId(receivable.getId());

        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirm_shouldSettleReceivableAndMarkPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var pix = createPixViaApi(receivable);

        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", pix.getTxid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        var settled = receivableRepository.findById(receivable.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(settled.getStatus()).isEqualTo(ReceivableStatus.PAID);
    }

    @Test
    void confirm_shouldReturn404WhenTxidUnknown() throws Exception {
        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirm_shouldReturn409WhenAlreadyPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var pix = createPixViaApi(receivable);
        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", pix.getTxid()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", pix.getTxid()))
                .andExpect(status().isConflict());
    }

    @Test
    void confirm_shouldRecordPaymentMovement() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var pix = createPixViaApi(receivable);
        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", pix.getTxid()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/finance/receivables/{id}/movements", receivable.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].movementType").value("PAYMENT"));
    }

    @Test
    void findAll_withReceptionistRole_shouldReturn200() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);
        createPixDirectly(createAndSaveReceivable(BigDecimal.valueOf(120)));

        mockMvc.perform(get("/finance/pix/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void confirm_shouldReturn409WhenExpired() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var pix = createPixViaApi(receivable);
        pix.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        pixPaymentRepository.save(pix);

        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", pix.getTxid()))
                .andExpect(status().isConflict());
    }

    @Test
    @Commit
    void confirm_shouldCancelAndPersistWhenReceivableAlreadySettled() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var pix = createPixViaApi(receivable);
        settleReceivableViaManualPayment(receivable);

        mockMvc.perform(post("/finance/pix/payments/{txid}/confirm", pix.getTxid()))
                .andExpect(status().isConflict());

        var persisted = pixPaymentRepository.findById(pix.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getStatus())
                .isEqualTo(PixPaymentStatus.CANCELLED);

        // Remove os dados commitados por este teste (@Commit) para não poluir o
        // banco compartilhado com as demais classes de teste.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            pixPaymentRepository.deleteAll();
            paymentRepository.deleteAll();
            movementRepository.deleteAll();
            receivableRepository.deleteAll();
            studentRepository.deleteAll();
            userRepository.deleteAll();
            studioRepository.deleteAll();
        });
    }

    @Test
    void create_shouldReturn409WhenReceivableHasPayment() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        settleReceivableViaManualPayment(receivable);

        var request = new PixPaymentRequest();
        request.setReceivableId(receivable.getId());

        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findAll_shouldReturnOnlyCurrentTenantPixCharges() throws Exception {
        createPixViaApi(createAndSaveReceivable(BigDecimal.valueOf(120)));

        Studio otherStudio = studioRepository.save(createStudio("Other Pix Studio"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));

        var request = new PixPaymentRequest();
        request.setReceivableId(otherReceivable.getId());
        mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/finance/pix/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].studentName").value("Pix Student"));
    }

    @Test
    void findById_shouldReturnPixChargeOfCurrentTenant() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(120));
        var pix = createPixViaApi(receivable);

        mockMvc.perform(get("/finance/pix/payments/{id}", pix.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pix.getId().toString()));
    }

    @Test
    void findById_shouldReturn404WhenPixBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));

        var otherPix = new PixPayment();
        otherPix.setStudio(otherStudio);
        otherPix.setReceivable(otherReceivable);
        otherPix.setTxid("OTHERTXID123");
        otherPix.setCopyPaste("000201...");
        otherPix.setAmount(BigDecimal.valueOf(80));
        otherPix.setStatus(PixPaymentStatus.PENDING);
        otherPix = pixPaymentRepository.save(otherPix);

        mockMvc.perform(get("/finance/pix/payments/{id}", otherPix.getId()))
                .andExpect(status().isNotFound());
    }

    private PixPayment createPixViaApi(Receivable receivable) throws Exception {
        var request = new PixPaymentRequest();
        request.setReceivableId(receivable.getId());

        String body = mockMvc.perform(post("/finance/pix/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return pixPaymentRepository.findById(
                UUID.fromString(objectMapper.readTree(body).get("id").asText())).orElseThrow();
    }

    private void settleReceivableViaManualPayment(Receivable receivable) throws Exception {
        var request = new br.com.corely.finance.payment.dto.PaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(receivable.getAmount());
        request.setPaymentMethod(br.com.corely.finance.payment.dto.PaymentMethodDto.CASH);

        mockMvc.perform(post("/finance/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private PixPayment createPixDirectly(Receivable receivable) {
        var pix = new PixPayment();
        pix.setStudio(studio);
        pix.setReceivable(receivable);
        pix.setTxid("DIRECTTXID" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        pix.setCopyPaste("000201...");
        pix.setAmount(receivable.getAmount());
        pix.setStatus(PixPaymentStatus.PENDING);
        pix.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
        return pixPaymentRepository.save(pix);
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
        user.setEmail("pix_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}