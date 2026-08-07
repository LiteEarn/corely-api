package br.com.corely.finance.card;

import br.com.corely.finance.card.dto.CardPaymentRequest;
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
 * Testes de integração do endpoint de transações de cartão (EPIC-03-S08).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CardPaymentControllerTest {

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
    private CardPaymentRepository cardPaymentRepository;

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
        // Limpeza executada dentro da transação de teste (REQUIRED). Como os
        // testes anotados com @Commit persistem dados que o rollback padrão não
        // reverte, cada teste @Commit também limpa seus próprios dados ao final,
        // garantindo o isolamento entre as classes de teste.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            cardPaymentRepository.deleteAll();
            paymentRepository.deleteAll();
            movementRepository.deleteAll();
            receivableRepository.deleteAll();
            studentRepository.deleteAll();
            userRepository.deleteAll();
            studioRepository.deleteAll();
        });

        studio = studioRepository.save(createStudio("Card Studio"));
        student = createAndSaveStudent(studio, "Card Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldReturn201WithTransactionId() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));

        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");
        request.setInstallments(1);

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receivableId").value(receivable.getId().toString()))
                .andExpect(jsonPath("$.studentName").value("Card Student"))
                .andExpect(jsonPath("$.amount").value(150))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.lastFourDigits").value("1234"));
    }

    @Test
    void create_shouldReturn404WhenReceivableBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(100));

        var request = new CardPaymentRequest();
        request.setReceivableId(otherReceivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409WhenReceivableAlreadyPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        receivable.setStatus(ReceivableStatus.PAID);
        receivableRepository.save(receivable);

        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409WhenReceivableAlreadyHasCardTransaction() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        createCardViaApi(receivable);

        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldEnforceUniqueReceivableConstraint() {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        createCardDirectly(receivable);

        var second = new CardPayment();
        second.setStudio(studio);
        second.setReceivable(receivable);
        second.setTransactionId("SECONDTXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        second.setCardBrand("VISA");
        second.setLastFourDigits("1234");
        second.setInstallments(1);
        second.setAmount(BigDecimal.valueOf(100));
        second.setStatus(CardPaymentStatus.PENDING);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> cardPaymentRepository.saveAndFlush(second))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void create_shouldReturn400WhenReceivableMissing() throws Exception {
        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenLastFourDigitsInvalid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));

        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("12");

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withReceptionistRole_shouldReturn403() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        var receivable = createAndSaveReceivable(BigDecimal.valueOf(100));
        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirm_shouldSettleReceivableAndMarkPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var card = createCardViaApi(receivable);

        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", card.getTransactionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        var settled = receivableRepository.findById(receivable.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(settled.getStatus()).isEqualTo(ReceivableStatus.PAID);
    }

    @Test
    void confirm_shouldReturn404WhenTransactionIdUnknown() throws Exception {
        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirm_shouldReturn409WhenAlreadyPaid() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var card = createCardViaApi(receivable);
        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", card.getTransactionId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", card.getTransactionId()))
                .andExpect(status().isConflict());
    }

    @Test
    void confirm_shouldRecordPaymentMovement() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var card = createCardViaApi(receivable);
        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", card.getTransactionId()))
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
        createCardDirectly(createAndSaveReceivable(BigDecimal.valueOf(120)));

        mockMvc.perform(get("/finance/card/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void confirm_shouldReturn409WhenExpired() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var card = createCardViaApi(receivable);
        card.setExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        cardPaymentRepository.save(card);

        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", card.getTransactionId()))
                .andExpect(status().isConflict());
    }

    @Test
    @Commit
    void confirm_shouldCancelAndPersistWhenReceivableAlreadySettled() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var card = createCardViaApi(receivable);
        settleReceivableViaManualPayment(receivable);

        mockMvc.perform(post("/finance/card/payments/{transactionId}/confirm", card.getTransactionId()))
                .andExpect(status().isConflict());

        var persisted = cardPaymentRepository.findById(card.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getStatus())
                .isEqualTo(CardPaymentStatus.CANCELLED);

        // Remove os dados commitados por este teste (@Commit) para não poluir o
        // banco compartilhado com as demais classes de teste.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            cardPaymentRepository.deleteAll();
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

        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");

        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findAll_shouldReturnOnlyCurrentTenantCardTransactions() throws Exception {
        createCardViaApi(createAndSaveReceivable(BigDecimal.valueOf(120)));

        Studio otherStudio = studioRepository.save(createStudio("Other Card Studio"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));

        var request = new CardPaymentRequest();
        request.setReceivableId(otherReceivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");
        mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/finance/card/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].studentName").value("Card Student"));
    }

    @Test
    void findById_shouldReturnCardTransactionOfCurrentTenant() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(120));
        var card = createCardViaApi(receivable);

        mockMvc.perform(get("/finance/card/payments/{id}", card.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId().toString()));
    }

    @Test
    void findById_shouldReturn404WhenCardBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));

        var otherCard = new CardPayment();
        otherCard.setStudio(otherStudio);
        otherCard.setReceivable(otherReceivable);
        otherCard.setTransactionId("OTHERTXN123");
        otherCard.setCardBrand("VISA");
        otherCard.setLastFourDigits("1234");
        otherCard.setInstallments(1);
        otherCard.setAmount(BigDecimal.valueOf(80));
        otherCard.setStatus(CardPaymentStatus.PENDING);
        otherCard = cardPaymentRepository.save(otherCard);

        mockMvc.perform(get("/finance/card/payments/{id}", otherCard.getId()))
                .andExpect(status().isNotFound());
    }

    private CardPayment createCardViaApi(Receivable receivable) throws Exception {
        var request = new CardPaymentRequest();
        request.setReceivableId(receivable.getId());
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");
        request.setInstallments(1);

        String body = mockMvc.perform(post("/finance/card/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return cardPaymentRepository.findById(
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

    private CardPayment createCardDirectly(Receivable receivable) {
        var card = new CardPayment();
        card.setStudio(studio);
        card.setReceivable(receivable);
        card.setTransactionId("DIRECTTXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        card.setCardBrand("VISA");
        card.setLastFourDigits("1234");
        card.setInstallments(1);
        card.setAmount(receivable.getAmount());
        card.setStatus(CardPaymentStatus.PENDING);
        card.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
        return cardPaymentRepository.save(card);
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
        user.setEmail("card_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}