package br.com.corely.comercial.ruledefinition;

import br.com.corely.comercial.ruledefinition.dto.RuleDefinitionRequest;
import br.com.corely.comercial.ruledefinition.dto.RuleDefinitionResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RuleDefinitionService {

    private final RuleDefinitionRepository repository;

    @Transactional
    public RuleDefinitionResponse create(RuleDefinitionRequest request) {
        if (repository.existsByCode(request.getCode())) {
            throw new BusinessException("Code already exists: " + request.getCode());
        }

        var entity = new RuleDefinition();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setValueType(request.getValueType());
        entity.setCategory(request.getCategory());
        entity.setRequired(request.getRequired() != null && request.getRequired());
        entity.setDefaultValue(request.getDefaultValue());
        entity.setActive(request.getActive() != null ? request.getActive() : true);

        entity = repository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<RuleDefinitionResponse> findAll() {
        return repository.findAll(Sort.by("name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RuleDefinitionResponse> findAllActive() {
        return repository.findByActiveTrueOrderByName().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RuleDefinitionResponse findById(UUID id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RuleDefinition not found"));
        return toResponse(entity);
    }

    @Transactional
    public RuleDefinitionResponse update(UUID id, RuleDefinitionRequest request) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RuleDefinition not found"));

        if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new BusinessException("Code already exists: " + request.getCode());
        }

        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setValueType(request.getValueType());
        entity.setCategory(request.getCategory());
        entity.setRequired(request.getRequired() != null && request.getRequired());
        entity.setDefaultValue(request.getDefaultValue());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        entity = repository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public void inactivate(UUID id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RuleDefinition not found"));
        if (!entity.getActive()) {
            throw new BusinessException("RuleDefinition is already inactive");
        }
        entity.setActive(false);
        repository.save(entity);
    }

    @Transactional
    public void activate(UUID id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RuleDefinition not found"));
        if (entity.getActive()) {
            throw new BusinessException("RuleDefinition is already active");
        }
        entity.setActive(true);
        repository.save(entity);
    }

    private RuleDefinitionResponse toResponse(RuleDefinition entity) {
        return new RuleDefinitionResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getValueType(),
                entity.getCategory(),
                entity.getRequired(),
                entity.getDefaultValue(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
