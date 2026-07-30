package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Métricas de clientes do mês consultado")
public class StudentMetricsResponse {

    @Schema(description = "Quantidade de alunos com contrato ativo", example = "120")
    private Long activeStudents;

    @Schema(description = "Quantidade de alunos inadimplentes (com faturas vencidas)", example = "8")
    private Long delinquentStudents;

    @Schema(description = "Quantidade de novos alunos no mês (contratos criados)", example = "15")
    private Long newStudentsThisMonth;

    @Schema(description = "Quantidade de cancelamentos no mês", example = "3")
    private Long cancellationsThisMonth;
}
