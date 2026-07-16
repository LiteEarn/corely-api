package br.com.corely.comercial.dashboard.dto;

import br.com.corely.comercial.classsession.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Sessão do Dashboard Diário")
public class SessionDashboardResponse {

    private UUID id;
    private LocalTime horario;
    private String professor;
    private String sala;
    private SessionStatus status;
    private Integer capacidade;
    private Integer vagasOcupadas;
    private Integer vagasDisponiveis;
    private Long totalPresencas;
    private Long totalFaltas;
    private Long quantidadeListaEspera;
    private Long quantidadeCreditosReposicao;

    public SessionDashboardResponse() {}

    public SessionDashboardResponse(UUID id, LocalTime horario, String professor, String sala,
                                    SessionStatus status, Integer capacidade, Integer vagasOcupadas,
                                    Integer vagasDisponiveis, Long totalPresencas, Long totalFaltas,
                                    Long quantidadeListaEspera, Long quantidadeCreditosReposicao) {
        this.id = id;
        this.horario = horario;
        this.professor = professor;
        this.sala = sala;
        this.status = status;
        this.capacidade = capacidade;
        this.vagasOcupadas = vagasOcupadas;
        this.vagasDisponiveis = vagasDisponiveis;
        this.totalPresencas = totalPresencas;
        this.totalFaltas = totalFaltas;
        this.quantidadeListaEspera = quantidadeListaEspera;
        this.quantidadeCreditosReposicao = quantidadeCreditosReposicao;
    }

    public UUID getId() { return id; }
    public LocalTime getHorario() { return horario; }
    public String getProfessor() { return professor; }
    public String getSala() { return sala; }
    public SessionStatus getStatus() { return status; }
    public Integer getCapacidade() { return capacidade; }
    public Integer getVagasOcupadas() { return vagasOcupadas; }
    public Integer getVagasDisponiveis() { return vagasDisponiveis; }
    public Long getTotalPresencas() { return totalPresencas; }
    public Long getTotalFaltas() { return totalFaltas; }
    public Long getQuantidadeListaEspera() { return quantidadeListaEspera; }
    public Long getQuantidadeCreditosReposicao() { return quantidadeCreditosReposicao; }
}
