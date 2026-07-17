package br.com.corely.comercial.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Dashboard Operacional Diário")
public class DailyDashboardResponse {

    private LocalDate dataConsultada;
    private Long quantidadeSessoes;
    private Long sessoesIniciadas;
    private Long sessoesFinalizadas;
    private Long sessoesCanceladas;
    private Integer totalVagas;
    private Integer vagasOcupadas;
    private Integer vagasLivres;
    private Long totalAlunosEsperados;
    private Long presentes;
    private Long faltas;
    private List<SessionDashboardResponse> sessoes;

    public DailyDashboardResponse() {}

    public DailyDashboardResponse(LocalDate dataConsultada, Long quantidadeSessoes,
                                   Long sessoesIniciadas, Long sessoesFinalizadas,
                                   Long sessoesCanceladas, Integer totalVagas,
                                   Integer vagasOcupadas, Integer vagasLivres,
                                   Long totalAlunosEsperados, Long presentes, Long faltas,
                                   List<SessionDashboardResponse> sessoes) {
        this.dataConsultada = dataConsultada;
        this.quantidadeSessoes = quantidadeSessoes;
        this.sessoesIniciadas = sessoesIniciadas;
        this.sessoesFinalizadas = sessoesFinalizadas;
        this.sessoesCanceladas = sessoesCanceladas;
        this.totalVagas = totalVagas;
        this.vagasOcupadas = vagasOcupadas;
        this.vagasLivres = vagasLivres;
        this.totalAlunosEsperados = totalAlunosEsperados;
        this.presentes = presentes;
        this.faltas = faltas;
        this.sessoes = sessoes;
    }

    public LocalDate getDataConsultada() { return dataConsultada; }
    public Long getQuantidadeSessoes() { return quantidadeSessoes; }
    public Long getSessoesIniciadas() { return sessoesIniciadas; }
    public Long getSessoesFinalizadas() { return sessoesFinalizadas; }
    public Long getSessoesCanceladas() { return sessoesCanceladas; }
    public Integer getTotalVagas() { return totalVagas; }
    public Integer getVagasOcupadas() { return vagasOcupadas; }
    public Integer getVagasLivres() { return vagasLivres; }
    public Long getTotalAlunosEsperados() { return totalAlunosEsperados; }
    public Long getPresentes() { return presentes; }
    public Long getFaltas() { return faltas; }
    public List<SessionDashboardResponse> getSessoes() { return sessoes; }
}
