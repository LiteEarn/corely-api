package br.com.corely.comercial.billingschedule;

import br.com.corely.comercial.billingschedule.dto.BillingFrequencyDto;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleRequest;
import br.com.corely.comercial.contract.ContractApplicationService;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.shared.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BillingScheduleIntegrationTest {

    @Autowired
    private BillingScheduleService billingScheduleService;

    @Autowired
    private BillingScheduleRepository billingScheduleRepository;

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
    private Plan plan;
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Billing Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "John Doe"));

        plan = planRepository.save(createPlan("Premium Plan", BigDecimal.valueOf(299), 30));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(plan.getId());
        spRequest.setStartDate(LocalDate.of(2026, 8, 15));
        studentPlanId = contractApplicationService.enroll(spRequest).getId();
    }

    @Test
    void create_shouldAutoCreateBillingSchedule() {
        var schedules = billingScheduleService.findAll();

        assertThat(schedules).hasSize(1);
        var schedule = schedules.get(0);
        assertThat(schedule.getStudentPlanId()).isEqualTo(studentPlanId);
        assertThat(schedule.getFrequency()).isEqualTo(BillingFrequencyDto.MONTHLY);
        assertThat(schedule.getBillingDay()).isEqualTo(15);
        assertThat(schedule.getActive()).isTrue();
        assertThat(schedule.getStudentName()).isEqualTo("John Doe");
        assertThat(schedule.getPlanName()).isEqualTo("Premium Plan");
    }

    @Test
    void shouldHaveOnlyOneBillingSchedulePerStudentPlan() {
        var schedules = billingScheduleService.findAll();
        assertThat(schedules).hasSize(1);
    }

    @Test
    void update_shouldChangeFrequency() {
        var schedule = billingScheduleService.findAll().get(0);

        var request = new BillingScheduleRequest();
        request.setFrequency(BillingFrequencyDto.QUARTERLY);
        request.setBillingDay(10);

        var response = billingScheduleService.update(schedule.getId(), request);

        assertThat(response.getFrequency()).isEqualTo(BillingFrequencyDto.QUARTERLY);
        assertThat(response.getBillingDay()).isEqualTo(10);
    }

    @Test
    void findById_shouldReturnBillingSchedule() {
        var schedule = billingScheduleService.findAll().get(0);

        var response = billingScheduleService.findById(schedule.getId());

        assertThat(response.getId()).isEqualTo(schedule.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> billingScheduleService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("BillingSchedule not found");
    }

    @Test
    void findAll_shouldReturnAllSchedules() {
        var all = billingScheduleService.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    void nextBillingDate_shouldBeCalculated() {
        var schedule = billingScheduleService.findAll().get(0);

        assertThat(schedule.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 8, 15));
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
