package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.planrule.dto.PlanRuleRequest;
import br.com.corely.comercial.planrule.PlanRuleService;
import br.com.corely.comercial.contract.ContractApplicationService;
import br.com.corely.comercial.ruledefinition.Category;
import br.com.corely.comercial.ruledefinition.RuleDefinition;
import br.com.corely.comercial.ruledefinition.RuleDefinitionRepository;
import br.com.corely.comercial.ruledefinition.ValueType;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.shared.exception.BusinessException;
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
class PlanServiceIntegrationTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private PlanRuleService planRuleService;

    @Autowired
    private ContractApplicationService contractApplicationService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanRuleRepository planRuleRepository;

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;

    @BeforeEach
    void setUp() {
        studentPlanRepository.deleteAll();
        planRuleRepository.deleteAll();
        planRepository.deleteAll();
        studentRepository.deleteAll();
        ruleDefinitionRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = studioRepository.save(createStudio("Plan Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);
    }

    @Test
    void create_shouldCreatePlanWithCounts() {
        var request = buildPlanRequest("Plano Basico", BigDecimal.valueOf(199), 1);

        var response = planService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Plano Basico");
        assertThat(response.getActiveStudentCount()).isEqualTo(0L);
        assertThat(response.getRuleCount()).isEqualTo(0L);
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldRejectDuplicateName() {
        planService.create(buildPlanRequest("Plano Mensal", BigDecimal.valueOf(250), 1));

        assertThatThrownBy(() ->
                planService.create(buildPlanRequest("Plano Mensal", BigDecimal.valueOf(300), 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plan name already exists");
    }

    @Test
    void findAll_shouldReturnPaginatedResults() {
        planService.create(buildPlanRequest("Plano A", BigDecimal.valueOf(100), 1));
        planService.create(buildPlanRequest("Plano B", BigDecimal.valueOf(200), 3));
        planService.create(buildPlanRequest("Plano C", BigDecimal.valueOf(300), 12));

        var result = planService.findAll(null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void findAll_shouldFilterByName() {
        planService.create(buildPlanRequest("Plano Mensal", BigDecimal.valueOf(250), 1));
        planService.create(buildPlanRequest("Plano Anual", BigDecimal.valueOf(2200), 12));

        var result = planService.findAll("Mensal", null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Plano Mensal");
    }

    @Test
    void findAll_shouldFilterByActive() {
        var created = planService.create(buildPlanRequest("Plano Ativo", BigDecimal.valueOf(100), 1));
        planService.create(buildPlanRequest("Plano Inativo", BigDecimal.valueOf(200), 3));
        planService.inactivate(planRepository.findByNameContainingIgnoreCase("Plano Inativo",
                org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0).getId());

        var activeResult = planService.findAll(null, true,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(activeResult.getTotalElements()).isEqualTo(1);

        var inactiveResult = planService.findAll(null, false,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(inactiveResult.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_shouldReturnPlanWithCounts() {
        var created = planService.create(buildPlanRequest("Plano Premium", BigDecimal.valueOf(399), 6));

        var ruleDef = ruleDefinitionRepository.save(createRuleDef("MAX_SESSIONS", ValueType.INTEGER));
        planRuleService.create(created.getId(), buildPlanRuleRequest(ruleDef.getId(), "20"));

        var student = studentRepository.save(createStudent("Joao Silva"));
        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(created.getId());
        spRequest.setStartDate(LocalDate.now());
        contractApplicationService.enroll(spRequest);

        var response = planService.findById(created.getId());

        assertThat(response.getActiveStudentCount()).isEqualTo(1L);
        assertThat(response.getRuleCount()).isEqualTo(1L);
    }

    @Test
    void update_shouldIncrementVersion() {
        var created = planService.create(buildPlanRequest("Plano Basico", BigDecimal.valueOf(199), 1));

        var updateRequest = buildPlanRequest("Plano Basico Plus", BigDecimal.valueOf(249), 2);
        var updated = planService.update(created.getId(), updateRequest);

        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getName()).isEqualTo("Plano Basico Plus");
        assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(249));
    }

    @Test
    void inactivate_shouldDeactivatePlan() {
        var created = planService.create(buildPlanRequest("Plano Basico", BigDecimal.valueOf(199), 1));

        planService.inactivate(created.getId());

        var response = planService.findById(created.getId());
        assertThat(response.getActive()).isFalse();
        assertThat(response.getVersion()).isEqualTo(2);
    }

    @Test
    void inactivate_shouldThrowWhenHasActiveContracts() {
        var created = planService.create(buildPlanRequest("Plano Basico", BigDecimal.valueOf(199), 1));

        var student = studentRepository.save(createStudent("Maria Santos"));
        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(created.getId());
        spRequest.setStartDate(LocalDate.now());
        contractApplicationService.enroll(spRequest);

        assertThatThrownBy(() -> planService.inactivate(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot inactivate plan with active student contracts");
    }

    @Test
    void activate_shouldActivatePlan() {
        var created = planService.create(buildPlanRequest("Plano Basico", BigDecimal.valueOf(199), 1));
        planService.inactivate(created.getId());

        planService.activate(created.getId());

        var response = planService.findById(created.getId());
        assertThat(response.getActive()).isTrue();
        assertThat(response.getVersion()).isEqualTo(3);
    }

    @Test
    void delete_shouldDeletePlanWithoutContracts() {
        var created = planService.create(buildPlanRequest("Plano Temp", BigDecimal.valueOf(50), 1));

        planService.delete(created.getId());

        assertThat(planRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void delete_shouldDeletePlanAndAssociatedRules() {
        var created = planService.create(buildPlanRequest("Plano Temp", BigDecimal.valueOf(50), 1));
        var ruleDef = ruleDefinitionRepository.save(createRuleDef("TEST_RULE", ValueType.BOOLEAN));
        planRuleService.create(created.getId(), buildPlanRuleRequest(ruleDef.getId(), "true"));

        planService.delete(created.getId());

        assertThat(planRepository.findById(created.getId())).isEmpty();
        assertThat(planRuleRepository.findByPlanIdOrderByCreatedAt(created.getId())).isEmpty();
    }

    @Test
    void delete_shouldThrowWhenHasActiveContracts() {
        var created = planService.create(buildPlanRequest("Plano Basico", BigDecimal.valueOf(199), 1));

        var student = studentRepository.save(createStudent("Pedro Costa"));
        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(created.getId());
        spRequest.setStartDate(LocalDate.now());
        contractApplicationService.enroll(spRequest);

        assertThatThrownBy(() -> planService.delete(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete plan with active or suspended student contracts");
    }

    @Test
    void update_shouldRejectDuplicateName() {
        planService.create(buildPlanRequest("Plano A", BigDecimal.valueOf(100), 1));
        var planB = planService.create(buildPlanRequest("Plano B", BigDecimal.valueOf(200), 3));

        assertThatThrownBy(() ->
                planService.update(planB.getId(), buildPlanRequest("Plano A", BigDecimal.valueOf(200), 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plan name already exists");
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + UUID.randomUUID() + "@test.com");
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

    private Student createStudent(String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return student;
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

    private PlanRequest buildPlanRequest(String name, BigDecimal price, int duration) {
        var request = new PlanRequest();
        request.setName(name);
        request.setPrice(price);
        request.setDuration(duration);
        request.setAutoRenew(true);
        return request;
    }

    private PlanRuleRequest buildPlanRuleRequest(UUID ruleDefinitionId, String value) {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(ruleDefinitionId);
        request.setValue(value);
        return request;
    }
}
