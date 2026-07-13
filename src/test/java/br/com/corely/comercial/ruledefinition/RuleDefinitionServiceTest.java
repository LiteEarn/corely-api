package br.com.corely.comercial.ruledefinition;

import br.com.corely.comercial.ruledefinition.dto.RuleDefinitionRequest;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RuleDefinitionServiceTest {

    @Autowired
    private RuleDefinitionService service;

    @Autowired
    private RuleDefinitionRepository repository;

    private RuleDefinition existingRule;

    @BeforeEach
    void setUp() {
        var rule = new RuleDefinition();
        rule.setCode("MAX_STUDENTS");
        rule.setName("Maximum Students");
        rule.setValueType(ValueType.INTEGER);
        rule.setCategory(Category.GENERAL);
        rule.setRequired(true);
        rule.setDefaultValue("30");
        rule.setActive(true);
        existingRule = repository.save(rule);
    }

    @Test
    void create_shouldPersistRuleDefinition() {
        var request = new RuleDefinitionRequest();
        request.setCode("MIN_AGE");
        request.setName("Minimum Age");
        request.setValueType(ValueType.INTEGER);
        request.setCategory(Category.VALIDITY);
        request.setRequired(true);
        request.setDefaultValue("18");

        var response = service.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getCode()).isEqualTo("MIN_AGE");
        assertThat(response.getName()).isEqualTo("Minimum Age");
        assertThat(response.getValueType()).isEqualTo(ValueType.INTEGER);
        assertThat(response.getCategory()).isEqualTo(Category.VALIDITY);
        assertThat(response.getRequired()).isTrue();
        assertThat(response.getDefaultValue()).isEqualTo("18");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenCodeAlreadyExists() {
        var request = new RuleDefinitionRequest();
        request.setCode("MAX_STUDENTS");
        request.setName("Duplicate");
        request.setValueType(ValueType.STRING);
        request.setCategory(Category.GENERAL);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Code already exists");
    }

    @Test
    void create_shouldSetDefaultActiveTrue_whenNotProvided() {
        var request = new RuleDefinitionRequest();
        request.setCode("NEW_RULE");
        request.setName("New Rule");
        request.setValueType(ValueType.BOOLEAN);
        request.setCategory(Category.BILLING);

        var response = service.create(request);

        assertThat(response.getActive()).isTrue();
    }

    @Test
    void findById_shouldReturnRule() {
        var response = service.findById(existingRule.getId());

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("MAX_STUDENTS");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("RuleDefinition not found");
    }

    @Test
    void findAllActive_shouldReturnOnlyActiveRules() {
        var inactive = new RuleDefinition();
        inactive.setCode("INACTIVE_RULE");
        inactive.setName("Inactive Rule");
        inactive.setValueType(ValueType.STRING);
        inactive.setCategory(Category.GENERAL);
        inactive.setActive(false);
        repository.save(inactive);

        var activeRules = service.findAllActive();

        assertThat(activeRules).hasSize(1);
        assertThat(activeRules.get(0).getCode()).isEqualTo("MAX_STUDENTS");
    }

    @Test
    void findAll_shouldReturnAllRules() {
        var inactive = new RuleDefinition();
        inactive.setCode("INACTIVE_RULE");
        inactive.setName("Inactive Rule");
        inactive.setValueType(ValueType.STRING);
        inactive.setCategory(Category.GENERAL);
        inactive.setActive(false);
        repository.save(inactive);

        var allRules = service.findAll();

        assertThat(allRules).hasSize(2);
    }

    @Test
    void update_shouldModifyRuleDefinition() {
        var request = new RuleDefinitionRequest();
        request.setCode("UPDATED_CODE");
        request.setName("Updated Name");
        request.setValueType(ValueType.DECIMAL);
        request.setCategory(Category.BILLING);
        request.setRequired(false);
        request.setDefaultValue("10.5");

        var response = service.update(existingRule.getId(), request);

        assertThat(response.getCode()).isEqualTo("UPDATED_CODE");
        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getValueType()).isEqualTo(ValueType.DECIMAL);
        assertThat(response.getCategory()).isEqualTo(Category.BILLING);
        assertThat(response.getRequired()).isFalse();
        assertThat(response.getDefaultValue()).isEqualTo("10.5");
    }

    @Test
    void update_shouldThrowException_whenCodeAlreadyExists() {
        var another = new RuleDefinition();
        another.setCode("ANOTHER_CODE");
        another.setName("Another");
        another.setValueType(ValueType.STRING);
        another.setCategory(Category.GENERAL);
        another.setActive(true);
        repository.save(another);

        var request = new RuleDefinitionRequest();
        request.setCode("ANOTHER_CODE");
        request.setName("Updated");
        request.setValueType(ValueType.STRING);
        request.setCategory(Category.GENERAL);

        assertThatThrownBy(() -> service.update(existingRule.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Code already exists");
    }

    @Test
    void update_shouldAllowSameCodeForSameEntity() {
        var request = new RuleDefinitionRequest();
        request.setCode("MAX_STUDENTS");
        request.setName("Updated Name");
        request.setValueType(ValueType.INTEGER);
        request.setCategory(Category.GENERAL);

        var response = service.update(existingRule.getId(), request);

        assertThat(response.getCode()).isEqualTo("MAX_STUDENTS");
        assertThat(response.getName()).isEqualTo("Updated Name");
    }

    @Test
    void inactivate_shouldSetActiveFalse() {
        service.inactivate(existingRule.getId());

        var entity = repository.findById(existingRule.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void inactivate_shouldThrowException_whenAlreadyInactive() {
        service.inactivate(existingRule.getId());

        assertThatThrownBy(() -> service.inactivate(existingRule.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RuleDefinition is already inactive");
    }

    @Test
    void activate_shouldSetActiveTrue() {
        service.inactivate(existingRule.getId());

        service.activate(existingRule.getId());

        var entity = repository.findById(existingRule.getId()).orElseThrow();
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    void activate_shouldThrowException_whenAlreadyActive() {
        assertThatThrownBy(() -> service.activate(existingRule.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RuleDefinition is already active");
    }

    @Test
    void inactivate_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> service.inactivate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activate_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> service.activate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new RuleDefinitionRequest();
        request.setCode("NONEXISTENT");
        request.setName("Nonexistent");
        request.setValueType(ValueType.STRING);
        request.setCategory(Category.GENERAL);

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
