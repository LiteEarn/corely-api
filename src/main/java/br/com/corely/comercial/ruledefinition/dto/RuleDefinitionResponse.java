package br.com.corely.comercial.ruledefinition.dto;

import br.com.corely.comercial.ruledefinition.Category;
import br.com.corely.comercial.ruledefinition.ValueType;

import java.time.LocalDateTime;
import java.util.UUID;

public class RuleDefinitionResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private ValueType valueType;
    private Category category;
    private Boolean required;
    private String defaultValue;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RuleDefinitionResponse() {}

    public RuleDefinitionResponse(UUID id, String code, String name, String description,
                                  ValueType valueType, Category category, Boolean required,
                                  String defaultValue, Boolean active,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.valueType = valueType;
        this.category = category;
        this.required = required;
        this.defaultValue = defaultValue;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ValueType getValueType() { return valueType; }
    public Category getCategory() { return category; }
    public Boolean getRequired() { return required; }
    public String getDefaultValue() { return defaultValue; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
