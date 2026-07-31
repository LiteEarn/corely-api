package br.com.corely.comercial.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta completa de um plano comercial")
public class PlanResponse {

    @Schema(description = "ID do plano")
    private UUID id;

    @Schema(description = "Nome do plano")
    private String name;

    @Schema(description = "Descricao do plano")
    private String description;

    @Schema(description = "Preco do plano")
    private BigDecimal price;

    @Schema(description = "Duracao em meses")
    private Integer duration;

    @Schema(description = "Versao do plano (incrementada a cada alteracao)")
    private Integer version;

    @Schema(description = "Se o plano esta ativo")
    private Boolean active;

    @Schema(description = "Se o plano renova automaticamente")
    private Boolean autoRenew;

    @Schema(description = "Quantidade de alunos com plano ativo")
    private Long activeStudentCount;

    @Schema(description = "Quantidade de regras associadas ao plano")
    private Long ruleCount;

    @Schema(description = "Data de criacao")
    private LocalDateTime createdAt;

    @Schema(description = "Data da ultima atualizacao")
    private LocalDateTime updatedAt;

    public PlanResponse() {}

    public PlanResponse(UUID id, String name, String description, BigDecimal price,
                        Integer duration, Integer version, Boolean active, Boolean autoRenew,
                        Long activeStudentCount, Long ruleCount,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.duration = duration;
        this.version = version;
        this.active = active;
        this.autoRenew = autoRenew;
        this.activeStudentCount = activeStudentCount;
        this.ruleCount = ruleCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getDuration() { return duration; }
    public Integer getVersion() { return version; }
    public Boolean getActive() { return active; }
    public Boolean getAutoRenew() { return autoRenew; }
    public Long getActiveStudentCount() { return activeStudentCount; }
    public Long getRuleCount() { return ruleCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
