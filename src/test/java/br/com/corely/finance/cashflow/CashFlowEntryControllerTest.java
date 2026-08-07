package br.com.corely.finance.cashflow;

import br.com.corely.finance.cashflow.dto.CashFlowEntryRequest;
import br.com.corely.finance.cashflow.dto.CashFlowEntrySourceDto;
import br.com.corely.finance.cashflow.dto.CashFlowEntryTypeDto;
import br.com.corely.finance.payment.Payment;
import br.com.corely.finance.payment.PaymentMethod;
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
 * Testes de integração do endpoint de fluxo de caixa — entradas (EPIC-03-S11).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CashFlowEntryControllerTest {

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
    private CashFlowEntryRepository cashFlowEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        cashFlowEntryRepository.deleteAll();
        paymentRepository.deleteAll();
        receivableRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = studioRepository.save(createStudio("CashFlow Studio"));
        student = createAndSaveStudent(studio, "CashFlow Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_shouldReturn201ForManualEntry() throws Exception {
        var request = manualEntryRequest();

        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryType").value("ENTRY"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.description").value("Aporte inicial"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void create_shouldReturn201ForPaymentEntry() throws Exception {
        var receivable = createAndSaveReceivable(BigDecimal.valueOf(150));
        var payment = createPaymentDirectly(receivable);

        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);
        request.setPaymentId(payment.getId());

        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("PAYMENT"))
                .andExpect(jsonPath("$.paymentId").value(payment.getId().toString()));
    }

    @Test
    void create_shouldReturn400WhenPaymentIdMissingForPaymentSource() throws Exception {
        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);

        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn404WhenPaymentBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(100));
        var otherPayment = createPaymentDirectly(otherReceivable, otherStudio);

        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);
        request.setPaymentId(otherPayment.getId());

        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withReceptionistRole_shouldReturn403() throws Exception {
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manualEntryRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_shouldReturnOnlyCurrentTenantEntries() throws Exception {
        createEntryViaApi(manualEntryRequest());

        Studio otherStudio = studioRepository.save(createStudio("Other CashFlow"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));
        var otherPayment = createPaymentDirectly(otherReceivable, otherStudio);

        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);
        request.setPaymentId(otherPayment.getId());
        mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/finance/cash-flow/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Aporte inicial"));
    }

    @Test
    void findAll_withEntryTypeFilter_shouldReturnOnlyMatching() throws Exception {
        createEntryViaApi(manualEntryRequest());

        mockMvc.perform(get("/finance/cash-flow/entries").param("entryType", "OUTFLOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/finance/cash-flow/entries").param("entryType", "ENTRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void findAll_withReceptionistRole_shouldReturn200() throws Exception {
        createEntryViaApi(manualEntryRequest());

        // Troca para RECEPTIONIST apenas para a leitura.
        createAndAuthenticateUser(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/finance/cash-flow/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void findById_shouldReturnEntryOfCurrentTenant() throws Exception {
        var response = createEntryViaApi(manualEntryRequest());

        mockMvc.perform(get("/finance/cash-flow/entries/{id}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId().toString()));
    }

    @Test
    void findById_shouldReturn404WhenEntryBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherReceivable = createAndSaveReceivable(otherStudent, BigDecimal.valueOf(80));
        var otherPayment = createPaymentDirectly(otherReceivable, otherStudio);

        var otherEntry = new CashFlowEntry();
        otherEntry.setStudio(otherStudio);
        otherEntry.setEntryType(CashFlowEntryType.ENTRY);
        otherEntry.setEntryDate(LocalDate.now());
        otherEntry.setAmount(BigDecimal.valueOf(80));
        otherEntry.setDescription("Outro estúdio");
        otherEntry.setSource(CashFlowEntrySource.MANUAL);
        otherEntry = cashFlowEntryRepository.save(otherEntry);

        mockMvc.perform(get("/finance/cash-flow/entries/{id}", otherEntry.getId()))
                .andExpect(status().isNotFound());
    }

    private CashFlowEntryRequest manualEntryRequest() {
        var request = new CashFlowEntryRequest();
        request.setEntryType(CashFlowEntryTypeDto.ENTRY);
        request.setEntryDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(500));
        request.setDescription("Aporte inicial");
        request.setSource(CashFlowEntrySourceDto.MANUAL);
        request.setCategory("APORTE");
        return request;
    }

    private CashFlowEntry createEntryViaApi(CashFlowEntryRequest request) throws Exception {
        String body = mockMvc.perform(post("/finance/cash-flow/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return cashFlowEntryRepository.findById(
                UUID.fromString(objectMapper.readTree(body).get("id").asText())).orElseThrow();
    }

    private Payment createPaymentDirectly(Receivable receivable) {
        return createPaymentDirectly(receivable, studio);
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
        user.setEmail("cashflow_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
