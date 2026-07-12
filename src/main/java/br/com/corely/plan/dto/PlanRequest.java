package br.com.corely.plan.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Type is required")
    private PlanType type;

    @NotNull(message = "Value is required")
    @Positive(message = "Value must be greater than 0")
    private BigDecimal value;

    @Positive(message = "Quantity must be greater than 0")
    private Integer quantityAulas;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than 0")
    private Integer duration;

    private Boolean active = true;
}
