package br.com.corely.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanResponse {

    private UUID id;
    private UUID studioId;
    private String name;
    private String description;
    private PlanType type;
    private BigDecimal value;
    private Integer quantityAulas;
    private Integer duration;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
