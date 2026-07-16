# STORY-027 — Agenda Operacional do Dia

## Objetivo

Disponibilizar um endpoint consolidado contendo todas as informações necessárias para operação diária do estúdio.

Este endpoint será utilizado pelo Dashboard Operacional do Frontend.

Não criar novas entidades.

Esta história é apenas de leitura.

## Endpoint

`GET /comercial/dashboard/daily`

## Parâmetros

- `date` (opcional, formato yyyy-MM-dd) — Caso não informado, utiliza `LocalDate.now()`

## Resposta

`DailyDashboardResponse`:
- `dataConsultada` (LocalDate)
- `quantidadeSessoes` (Long)
- `sessoesIniciadas` (Long)
- `sessoesFinalizadas` (Long)
- `sessoesCanceladas` (Long)
- `totalVagas` (Integer)
- `vagasOcupadas` (Integer)
- `vagasLivres` (Integer)
- `totalAlunosEsperados` (Long)
- `presentes` (Long)
- `faltas` (Long)
- `sessoes` (List<SessionDashboardResponse>)

### SessionDashboardResponse
- `id` (UUID)
- `horario` (LocalTime)
- `professor` (String) — nome da agenda
- `sala` (String) — null (não disponível no modelo atual)
- `status` (SessionStatus)
- `capacidade` (Integer)
- `vagasOcupadas` (Integer)
- `vagasDisponiveis` (Integer)
- `totalPresencas` (Long)
- `totalFaltas` (Long)
- `quantidadeListaEspera` (Long)
- `quantidadeCreditosReposicao` (Long)

## Performance

- Consulta única com JOIN FETCH para carregar sessões com schedule/slot
- Consultas agregadas em batch (evitando N+1)
- Projeções com COUNT e GROUP BY
- Nenhuma entidade completa carregada para estatísticas

## Multi-tenant

Obrigatório — utiliza o `@Filter` do Hibernate via `TenantInterceptor`.

## Segurança

OWNER, ADMIN, RECEPTIONIST, FINANCIAL — somente leitura.

## Swagger

Documentado via `@Tag(name = "Dashboard")` e `@Operation`.

## Testes

- Testes unitários do serviço
- Testes de controller (MockMvc)
- Testes de isolamento de tenant
