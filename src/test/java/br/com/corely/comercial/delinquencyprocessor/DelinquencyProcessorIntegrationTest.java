package br.com.corely.comercial.delinquencyprocessor;

import br.com.corely.comercial.contract.ContractApplicationService;
import br.com.corely.comercial.delinquencypolicy.DelinquencyAction;
import br.com.corely.comercial.delinquencypolicy.DelinquencyPolicy;
import br.com.corely.comercial.delinquencypolicy.DelinquencyPolicyRepository;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceService;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.invoice.dto.InvoiceRequest;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DelinquencyProcessorIntegrationTest {

    @Autowired
    private DelinquencyProcessorService delinquencyProcessorService;

    @Autowired
    private DelinquencyPolicyRepository delinquencyPolicyRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ContractApplicationService contractApplicationService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanRuleRepository planRuleRepository;

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Delinquency Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "Jane Doe"));

        var plan = planRepository.save(createPlan("Basic Plan", BigDecimal.valueOf(199), 30));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(plan.getId());
        spRequest.setStartDate(LocalDate.of(2026, 1, 15));
        studentPlanId = contractApplicationService.enroll(spRequest).getId();
    }

    private UUID createInvoice(LocalDate dueDate) {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(dueDate);
        request.setReferenceMonth(dueDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        var response = invoiceService.create(request);
        var invoice = invoiceRepository.findById(response.getId()).orElseThrow();
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);
        return response.getId();
    }

    private UUID createPolicy(DelinquencyAction action, Integer gracePeriodDays) {
        var policy = new DelinquencyPolicy();
        policy.setStudio(studio);
        policy.setAction(action);
        policy.setGracePeriodDays(gracePeriodDays);
        policy.setActive(true);
        return delinquencyPolicyRepository.save(policy).getId();
    }

    @Test
    void process_shouldSuspendContractWhenDelinquent() {
        createInvoice(LocalDate.of(2026, 1, 15));
        createPolicy(DelinquencyAction.SUSPEND_CONTRACT, 5);

        var result = delinquencyProcessorService.process(LocalDate.of(2026, 1, 25));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getBlocked()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldNotSuspendWithinGracePeriod() {
        createInvoice(LocalDate.of(2026, 1, 15));
        createPolicy(DelinquencyAction.SUSPEND_CONTRACT, 15);

        var result = delinquencyProcessorService.process(LocalDate.of(2026, 1, 25));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldBlockNewBookingsWhenActionIsBlock() {
        createInvoice(LocalDate.of(2026, 1, 15));
        createPolicy(DelinquencyAction.BLOCK_NEW_BOOKINGS, 5);

        var result = delinquencyProcessorService.process(LocalDate.of(2026, 1, 25));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getBlocked()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldSkipWhenNoOverdueInvoices() {
        createPolicy(DelinquencyAction.SUSPEND_CONTRACT, 5);

        var result = delinquencyProcessorService.process(LocalDate.of(2026, 1, 25));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldSkipWhenNoPolicyExists() {
        createInvoice(LocalDate.of(2026, 1, 15));

        var result = delinquencyProcessorService.process(LocalDate.of(2026, 1, 25));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldNotProcessNonActivePlans() {
        createInvoice(LocalDate.of(2026, 1, 15));
        createPolicy(DelinquencyAction.SUSPEND_CONTRACT, 5);

        var result = delinquencyProcessorService.process(LocalDate.of(2026, 1, 25));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getSuspended()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);
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

    private Student createStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return student;
    }

    private Plan createPlan(String name, BigDecimal price, Integer duration) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        return plan;
    }

    private RuleDefinition createRuleDef(String code, ValueType valueType) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(code);
        rule.setValueType(valueType);
        rule.setCategory(Category.GENERAL);
        rule.setActive(true);
        return rule;
    }

    private PlanRule createPlanRule(Plan plan, RuleDefinition ruleDef, String value) {
        var pr = new PlanRule();
        pr.setStudio(studio);
        pr.setPlan(plan);
        pr.setRuleDefinition(ruleDef);
        pr.setValue(value);
        return pr;
    }
}