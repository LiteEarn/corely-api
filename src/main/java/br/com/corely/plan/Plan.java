package br.com.corely.plan;

import br.com.corely.plan.dto.PlanType;
import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.studio.Studio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Plan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PlanType type;

    @Column(name = "plan_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "quantity_aulas")
    private Integer quantityAulas;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
