# Corely — Backend Roadmap (Go Live)

> **Este é o roadmap executável do Corely.**
> Fonte oficial do planejamento do backend. Cada épico e cada story possuem
> metadados estruturados em blocos `yaml` embutidos no Markdown para leitura
> determinística por agentes de IA (sem heurísticas).
>
> - **Status**: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `BLOCKED` | `DONE`
> - **Priority**: `CRITICAL` | `HIGH` | `MEDIUM` | `LOW`
> - **Progress**: `0` a `100` (percentual do épico)

---

## ✅ Concluído (baseline)

### Plataforma

- Autenticação JWT
- RBAC (Roles/Permissions)
- Estrutura modular por domínio
- Flyway
- PostgreSQL
- Swagger/OpenAPI
- Base de testes

### Domínios

- Studios
- Usuários
- Alunos
- Instrutores
- Turmas
- Matrículas
- Presença
- Agenda (base)
- Avaliações
- Evoluções
- Objetivos
- Reposições
- Comercial (Planos)
- StudentPlanResponse enriquecido
- Billing Schedule
- Hardening do Contract Snapshot

---

# EPIC 01 — Multi-Tenant (Bloqueador Go Live)

**Status:** DONE · **Priority:** CRITICAL · **Progress:** 100%

Isolamento total de dados por Studio. Nenhum usuário acessa dados de outro studio.

```yaml
id: EPIC-01
title: Multi-Tenant
description: Isolamento completo de dados por Studio. Nenhum usuário acessa dados de outro studio.
status: DONE
priority: CRITICAL
progress: 100
dependsOn: []
acceptanceCriteria:
  - Nenhum usuário acessa dados de outro studio.
  - Todos os findAll() possuem filtro de tenant.
  - DTOs não recebem studioId controlado pelo client.
  - Testes de isolamento entre tenants passando.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes unitários e de integração verdes.
  - Roadmap atualizado e PROJECT_STATUS refletindo o progresso.
  - Sem N+1 e sem quebra de contrato REST.
stories:
  - id: EPIC-01-S01
    title: Contexto global de Tenant
    description: Implementar contexto global de tenant resolvendo o studioId corrente na requisição.
    status: DONE
    priority: CRITICAL
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - O studioId corrente é resolvido de forma confiável por requisição.
      - Contexto é reutilizado pelos filtros de tenant.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/java/br/com/corely/shared/tenant/TenantContext.java
      - src/main/java/br/com/corely/shared/tenant/TenantResolutionException.java
      - src/main/java/br/com/corely/comercial/tenant/ComercialTenantContext.java
      - src/main/java/br/com/corely/comercial/tenant/TenantInterceptor.java
      - src/test/java/br/com/corely/shared/tenant/TenantContextTest.java
      - src/test/java/br/com/corely/comercial/tenant/ComercialTenantContextTest.java
  - id: EPIC-01-S02
    title: Eliminar findAll() sem filtro
    description: Auditar e eliminar consultas findAll() sem filtro de tenant nas entidades multi-tenant.
    status: DONE
    priority: CRITICAL
    estimate: L
    dependsOn: [EPIC-01-S01]
    acceptanceCriteria:
      - Nenhuma consulta findAll() sem filtro permanece em entidades multi-tenant.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/java/br/com/corely/instructor/InstructorRepository.java
      - src/main/java/br/com/corely/instructor/InstructorService.java
      - src/main/java/br/com/corely/enrollment/EnrollmentRepository.java
      - src/main/java/br/com/corely/enrollment/EnrollmentService.java
      - src/main/java/br/com/corely/makeup/MakeupRequestRepository.java
      - src/main/java/br/com/corely/makeup/MakeupRequestService.java
      - src/main/java/br/com/corely/student/StudentService.java
      - src/main/java/br/com/corely/classgroup/ClassGroupRepository.java
      - src/main/java/br/com/corely/classgroup/ClassGroupService.java
      - src/main/java/br/com/corely/evaluation/EvaluationService.java
      - src/main/java/br/com/corely/evolution/EvolutionService.java
      - src/test/java/br/com/corely/classgroup/ClassGroupServiceTest.java
      - src/test/java/br/com/corely/makeup/MakeupRequestServiceTest.java
      - src/test/java/br/com/corely/auth/authorization/AuthorizationInterceptorTest.java
      - src/test/java/br/com/corely/evolution/EvolutionControllerTest.java
  - id: EPIC-01-S03
    title: Enforce por studio_id
    description: Garantir enforcement de studio_id em todas as consultas JPQL e filtros Hibernate.
    status: DONE
    priority: CRITICAL
    estimate: M
    dependsOn: [EPIC-01-S01]
    acceptanceCriteria:
      - Toda consulta respeita o isolamento por studio_id.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/java/br/com/corely/comercial/tenant/TenantInterceptor.java
      - src/main/java/br/com/corely/comercial/tenant/ComercialTenantContext.java
      - src/main/java/br/com/corely/shared/tenant/TenantContext.java
      - src/main/java/br/com/corely/student/Student.java
      - src/main/java/br/com/corely/instructor/Instructor.java
      - src/main/java/br/com/corely/classgroup/ClassGroup.java
      - src/main/java/br/com/corely/enrollment/Enrollment.java
      - src/main/java/br/com/corely/evaluation/Evaluation.java
      - src/main/java/br/com/corely/evolution/Evolution.java
      - src/main/java/br/com/corely/objective/Objective.java
      - src/main/java/br/com/corely/objective/ObjectiveRepository.java
      - src/main/java/br/com/corely/objective/ObjectiveService.java
      - src/main/java/br/com/corely/booking/Booking.java
      - src/main/java/br/com/corely/booking/BookingRepository.java
      - src/main/java/br/com/corely/booking/BookingService.java
      - src/main/java/br/com/corely/booking/TimeBlock.java
      - src/main/java/br/com/corely/user/User.java
      - src/main/java/br/com/corely/classsession/ClassSessionRepository.java
      - src/main/java/br/com/corely/classsession/ClassSessionSpecification.java
      - src/main/java/br/com/corely/classsession/ClassSessionService.java
      - src/main/java/br/com/corely/attendance/AttendanceRepository.java
      - src/main/java/br/com/corely/attendance/AttendanceService.java
      - src/main/java/br/com/corely/makeup/MakeupRequestRepository.java
      - src/main/java/br/com/corely/makeup/MakeupRequestService.java
      - src/test/java/br/com/corely/attendance/AttendanceControllerTest.java
      - src/test/java/br/com/corely/attendance/AttendanceServiceTest.java
      - src/test/java/br/com/corely/classsession/ClassSessionServiceTest.java
      - src/test/java/br/com/corely/dashboard/DashboardControllerTest.java
  - id: EPIC-01-S04
    title: Dashboard sem studioId vindo da URL
    description: Remover dependência de studioId informado na URL; derivar do contexto autenticado.
    status: DONE
    priority: CRITICAL
    estimate: S
    dependsOn: [EPIC-01-S01]
    acceptanceCriteria:
      - Dashboards usam o studioId do contexto, nunca da URL.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/java/br/com/corely/dashboard/DashboardController.java
      - src/main/java/br/com/corely/dashboard/DashboardService.java
      - src/main/java/br/com/corely/booking/BookingController.java
      - src/main/java/br/com/corely/booking/BookingService.java
      - src/test/java/br/com/corely/dashboard/DashboardControllerTest.java
      - src/test/java/br/com/corely/booking/BookingServiceTest.java
      - src/test/java/br/com/corely/auth/authorization/AuthorizationInterceptorTest.java
  - id: EPIC-01-S05
    title: DTOs sem studioId client-controlled
    description: Garantir que DTOs de request não aceitem studioId controlado pelo client.
    status: DONE
    priority: CRITICAL
    estimate: S
    dependsOn: [EPIC-01-S01]
    acceptanceCriteria:
      - Nenhum DTO de request aceita studioId controlado pelo client.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/java/br/com/corely/student/dto/StudentRequest.java
      - src/main/java/br/com/corely/instructor/dto/InstructorRequest.java
      - src/main/java/br/com/corely/classgroup/dto/ClassGroupRequest.java
      - src/main/java/br/com/corely/objective/dto/ObjectiveRequest.java
      - src/main/java/br/com/corely/evaluation/dto/EvaluationRequest.java
      - src/main/java/br/com/corely/evolution/dto/EvolutionRequest.java
      - src/main/java/br/com/corely/booking/dto/BookingRequest.java
      - src/main/java/br/com/corely/booking/dto/TimeBlockRequest.java
      - src/main/java/br/com/corely/enrollment/dto/EnrollmentRequest.java
      - src/main/java/br/com/corely/attendance/dto/BulkAttendanceRequest.java
      - src/main/java/br/com/corely/student/StudentService.java
      - src/main/java/br/com/corely/instructor/InstructorService.java
      - src/main/java/br/com/corely/classgroup/ClassGroupService.java
      - src/main/java/br/com/corely/objective/ObjectiveService.java
      - src/main/java/br/com/corely/evaluation/EvaluationService.java
      - src/main/java/br/com/corely/evolution/EvolutionService.java
      - src/main/java/br/com/corely/booking/BookingService.java
      - src/main/java/br/com/corely/booking/TimeBlockService.java
      - src/main/java/br/com/corely/enrollment/EnrollmentService.java
      - src/main/java/br/com/corely/attendance/AttendanceService.java
      - src/main/java/br/com/corely/attendance/AttendanceController.java
      - src/main/java/br/com/corely/dev/seed/SeedService.java
      - src/test/java/br/com/corely/student/StudentServiceTest.java
      - src/test/java/br/com/corely/instructor/InstructorServiceTest.java
      - src/test/java/br/com/corely/classgroup/ClassGroupServiceTest.java
      - src/test/java/br/com/corely/enrollment/EnrollmentServiceTest.java
      - src/test/java/br/com/corely/evolution/EvolutionControllerTest.java
      - src/test/java/br/com/corely/booking/BookingServiceTest.java
      - src/test/java/br/com/corely/booking/BookingControllerTest.java
      - src/test/java/br/com/corely/attendance/AttendanceServiceTest.java
  - id: EPIC-01-S06
    title: Testes de isolamento entre tenants
    description: Criar testes que comprovam que um studio não acessa dados de outro.
    status: DONE
    priority: CRITICAL
    estimate: L
    dependsOn: [EPIC-01-S01, EPIC-01-S02, EPIC-01-S03]
    acceptanceCriteria:
      - Testes de isolamento entre tenants passando para todos os módulos multi-tenant.
    definitionOfDone:
      - Testes verdes e documentação atualizada.
    files:
      - src/main/java/br/com/corely/student/StudentRepository.java
      - src/main/java/br/com/corely/instructor/InstructorRepository.java
      - src/main/java/br/com/corely/classgroup/ClassGroupRepository.java
      - src/main/java/br/com/corely/objective/ObjectiveRepository.java
      - src/main/java/br/com/corely/evaluation/EvaluationRepository.java
      - src/main/java/br/com/corely/evolution/EvolutionRepository.java
      - src/main/java/br/com/corely/enrollment/EnrollmentRepository.java
      - src/main/java/br/com/corely/booking/BookingRepository.java
      - src/main/java/br/com/corely/booking/TimeBlockRepository.java
      - src/test/java/br/com/corely/student/TenantIsolationTest.java
      - src/test/java/br/com/corely/instructor/TenantIsolationTest.java
      - src/test/java/br/com/corely/classgroup/TenantIsolationTest.java
      - src/test/java/br/com/corely/objective/TenantIsolationTest.java
      - src/test/java/br/com/corely/evaluation/TenantIsolationTest.java
      - src/test/java/br/com/corely/evolution/TenantIsolationTest.java
      - src/test/java/br/com/corely/enrollment/TenantIsolationTest.java
      - src/test/java/br/com/corely/classsession/TenantIsolationTest.java
      - src/test/java/br/com/corely/attendance/TenantIsolationTest.java
      - src/test/java/br/com/corely/makeup/TenantIsolationTest.java
      - src/test/java/br/com/corely/booking/TenantIsolationTest.java
```

---

# EPIC 02 — Segurança

**Status:** IN_PROGRESS · **Priority:** CRITICAL · **Progress:** 22%

Endurecimento de segurança da plataforma para produção.

```yaml
id: EPIC-02
title: Segurança
description: Endurecimento de segurança para produção.
status: IN_PROGRESS
priority: CRITICAL
progress: 33
dependsOn: [EPIC-01]
acceptanceCriteria:
  - Segredos fora de configuração versionada.
  - Produção protegida (Swagger, seed, rate limiting, CORS).
  - Auditoria LGPD implementada.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-02-S01
    title: JWT Secret via variável de ambiente
    description: Mover o JWT secret para variável de ambiente.
    status: DONE
    priority: CRITICAL
    estimate: S
    dependsOn: []
    acceptanceCriteria:
      - JWT secret nunca aparece em configuração versionada.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/resources/application.yaml
      - src/main/resources/application-dev.yaml
      - src/test/resources/application-test.properties
      - src/test/java/br/com/corely/auth/security/jwt/JwtSecretConfigTest.java
      - README.md
  - id: EPIC-02-S02
    title: Rotação de segredo
    description: Suportar rotação do segredo JWT sem downtime.
    status: DONE
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-02-S01]
    acceptanceCriteria:
      - Rotação de segredo documentada e funcional.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/java/br/com/corely/auth/security/jwt/JwtProperties.java
      - src/main/java/br/com/corely/auth/security/jwt/JwtService.java
      - src/main/resources/application.yaml
      - src/main/resources/application-dev.yaml
      - src/test/resources/application-test.properties
      - src/test/java/br/com/corely/auth/security/jwt/JwtServiceTest.java
      - src/test/java/br/com/corely/auth/security/jwt/JwtSecretConfigTest.java
      - README.md
  - id: EPIC-02-S03
    title: Profile production
    description: Configuração dedicada para o perfil de produção.
    status: DONE
    priority: CRITICAL
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Profile production isolado e seguro.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files:
      - src/main/resources/application-prod.yaml
      - src/main/resources/application.yaml
      - src/test/java/br/com/corely/auth/security/jwt/ProdProfileConfigTest.java
      - README.md
  - id: EPIC-02-S04
    title: Swagger protegido
    description: Proteger o Swagger/OpenAPI em produção.
    status: IN_PROGRESS
    priority: HIGH
    estimate: S
    dependsOn: [EPIC-02-S03]
    acceptanceCriteria:
      - Swagger inacessível em produção sem autenticação/autorização.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-02-S05
    title: Seed apenas em dev
    description: Garantir que o seed de dados ocorra apenas no perfil dev.
    status: TODO
    priority: CRITICAL
    estimate: S
    dependsOn: [EPIC-02-S03]
    acceptanceCriteria:
      - Seed desabilitado em produção.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-02-S06
    title: Rate limiting
    description: Adicionar rate limiting na API.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Rate limiting aplicado em endpoints sensíveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-02-S07
    title: Lockout login
    description: Implementar bloqueio após tentativas de login inválidas.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Lockout de login após tentativas consecutivas.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-02-S08
    title: CORS por ambiente
    description: Configurar CORS específico por ambiente.
    status: TODO
    priority: HIGH
    estimate: S
    dependsOn: [EPIC-02-S03]
    acceptanceCriteria:
      - CORS restrito por ambiente.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-02-S09
    title: Auditoria LGPD
    description: Implementar trilha de auditoria conforme requisitos LGPD.
    status: TODO
    priority: MEDIUM
    estimate: L
    dependsOn: [EPIC-02-S01]
    acceptanceCriteria:
      - Eventos relevantes auditados e rastreáveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 03 — Financeiro

**Status:** TODO · **Priority:** HIGH · **Progress:** 0%

Domínio financeiro completo: contas a receber, pagamentos, fluxo de caixa, inadimplência e dashboard financeiro.

```yaml
id: EPIC-03
title: Financeiro
description: "Domínio financeiro completo: contas a receber, pagamentos, fluxo de caixa, inadimplência e dashboard financeiro."
status: TODO
priority: HIGH
progress: 0
dependsOn: [EPIC-01, EPIC-02]
acceptanceCriteria:
  - Contas a receber, pagamentos, fluxo de caixa e inadimplência operacionais.
  - Dashboard financeiro com as métricas definidas.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-03-S01
    title: Contas a Receber — Recebíveis
    description: Modelar e expor recebíveis.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Recebíveis criados e consultáveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S02
    title: Contas a Receber — Parcelas
    description: Geração e gestão de parcelas.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S01]
    acceptanceCriteria:
      - Parcelas geradas a partir da matrícula/plano.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S03
    title: Contas a Receber — Situação
    description: Situação financeira de cada parcela/recebível.
    status: TODO
    priority: HIGH
    estimate: S
    dependsOn: [EPIC-03-S02]
    acceptanceCriteria:
      - Situação (em aberto, paga, vencida, estornada) disponível.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S04
    title: Contas a Receber — Vencimentos
    description: Controle de vencimentos.
    status: TODO
    priority: HIGH
    estimate: S
    dependsOn: [EPIC-03-S02]
    acceptanceCriteria:
      - Datas de vencimento controladas e consultáveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S05
    title: Contas a Receber — Histórico
    description: Histórico de movimentações do recebível.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S02]
    acceptanceCriteria:
      - Histórico de pagamentos e ajustes disponível.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S06
    title: Pagamentos — Baixa manual
    description: Baixa manual de pagamento.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S01]
    acceptanceCriteria:
      - Baixa manual registra o pagamento e atualiza a situação.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S07
    title: Pagamentos — Pix
    description: Integração/registro de pagamento via Pix.
    status: TODO
    priority: MEDIUM
    estimate: L
    dependsOn: [EPIC-03-S06]
    acceptanceCriteria:
      - Pagamento Pix registrado e conciliado.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S08
    title: Pagamentos — Cartão
    description: Integração/registro de pagamento via cartão.
    status: TODO
    priority: MEDIUM
    estimate: L
    dependsOn: [EPIC-03-S06]
    acceptanceCriteria:
      - Pagamento cartão registrado e conciliado.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S09
    title: Pagamentos — Dinheiro
    description: Registro de pagamento em dinheiro.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-03-S06]
    acceptanceCriteria:
      - Pagamento em dinheiro registrado.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S10
    title: Pagamentos — Estorno
    description: Estorno de pagamento.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S06]
    acceptanceCriteria:
      - Estorno registrado e refletido na situação.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S11
    title: Fluxo de Caixa — Entradas
    description: Entradas de caixa.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S01]
    acceptanceCriteria:
      - Entradas registradas e consultáveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S12
    title: Fluxo de Caixa — Saídas
    description: Saídas de caixa.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S11]
    acceptanceCriteria:
      - Saídas registradas e consultáveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S13
    title: Fluxo de Caixa — Saldo
    description: Cálculo do saldo de caixa.
    status: TODO
    priority: HIGH
    estimate: S
    dependsOn: [EPIC-03-S11, EPIC-03-S12]
    acceptanceCriteria:
      - Saldo calculado corretamente.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S14
    title: Fluxo de Caixa — Projeção
    description: Projeção de fluxo de caixa.
    status: TODO
    priority: MEDIUM
    estimate: L
    dependsOn: [EPIC-03-S13]
    acceptanceCriteria:
      - Projeção futura do caixa disponível.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S15
    title: Inadimplência — Cobranças vencidas
    description: Listagem de cobranças vencidas.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S01]
    acceptanceCriteria:
      - Cobranças vencidas listadas.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S16
    title: Inadimplência — Dias em atraso
    description: Cálculo de dias em atraso.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-03-S15]
    acceptanceCriteria:
      - Dias em atraso calculados.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S17
    title: Inadimplência — Alertas
    description: Alertas de inadimplência.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S15]
    acceptanceCriteria:
      - Alertas gerados para cobranças em atraso.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S18
    title: Dashboard Financeiro — Receita mensal
    description: Métrica de receita mensal.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S01]
    acceptanceCriteria:
      - Receita mensal calculada e exposta.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S19
    title: Dashboard Financeiro — Receita anual
    description: Métrica de receita anual.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S18]
    acceptanceCriteria:
      - Receita anual calculada e exposta.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S20
    title: Dashboard Financeiro — Ticket médio
    description: Métrica de ticket médio.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-03-S18]
    acceptanceCriteria:
      - Ticket médio calculado e exposto.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S21
    title: Dashboard Financeiro — MRR
    description: Métrica de MRR (receita recorrente mensal).
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-03-S18]
    acceptanceCriteria:
      - MRR calculado e exposto.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S22
    title: Dashboard Financeiro — Churn
    description: Métrica de churn.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S21]
    acceptanceCriteria:
      - Churn calculado e exposto.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S23
    title: Dashboard Financeiro — Receita por plano
    description: Métrica de receita por plano.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S18]
    acceptanceCriteria:
      - Receita por plano calculada e exposta.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-03-S24
    title: Dashboard Financeiro — Receita por instrutor
    description: Métrica de receita por instrutor.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-03-S18]
    acceptanceCriteria:
      - Receita por instrutor calculada e exposta.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 04 — Booking

**Status:** TODO · **Priority:** HIGH · **Progress:** 0%

Reservas, remarcações, disponibilidade e ocupação.

```yaml
id: EPIC-04
title: Booking
description: Reservas, remarcações, disponibilidade e ocupação.
status: TODO
priority: HIGH
progress: 0
dependsOn: [EPIC-01, EPIC-02]
acceptanceCriteria:
  - Reserva, remarcação, lista de espera e disponibilidade operacionais.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-04-S01
    title: Disponibilidade
    description: Consulta de disponibilidade de horários/turmas.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Disponibilidade consultada sem conflitos.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-04-S02
    title: Conflito de horário
    description: Detecção de conflito de horário nas reservas.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-04-S01]
    acceptanceCriteria:
      - Conflitos detectados e bloqueados.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-04-S03
    title: Lista de espera
    description: Gestão de lista de espera.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-04-S02]
    acceptanceCriteria:
      - Lista de espera operacional.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-04-S04
    title: Reserva
    description: Criação de reserva.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-04-S02]
    acceptanceCriteria:
      - Reserva criada respeitando disponibilidade.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-04-S05
    title: Remarcação
    description: Remarcação de reserva.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-04-S04]
    acceptanceCriteria:
      - Remarcação respeitando disponibilidade e conflitos.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-04-S06
    title: Ocupação
    description: Métrica de ocupação.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-04-S04]
    acceptanceCriteria:
      - Ocupação calculada e exposta.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 05 — Comunicação

**Status:** TODO · **Priority:** HIGH · **Progress:** 0%

WhatsApp, lembretes, confirmações, cancelamentos e cobranças.

```yaml
id: EPIC-05
title: Comunicação
description: WhatsApp, lembretes, confirmações, cancelamentos e cobranças.
status: TODO
priority: HIGH
progress: 0
dependsOn: [EPIC-03, EPIC-04]
acceptanceCriteria:
  - Comunicação via WhatsApp com templates e automações operacionais.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-05-S01
    title: WhatsApp
    description: Integração com WhatsApp para envio de mensagens.
    status: TODO
    priority: HIGH
    estimate: L
    dependsOn: []
    acceptanceCriteria:
      - Envio de mensagens WhatsApp funcional.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-05-S02
    title: Confirmação automática
    description: Confirmação automática de agendamentos.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-05-S01]
    acceptanceCriteria:
      - Confirmações enviadas automaticamente.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-05-S03
    title: Lembretes
    description: Lembretes de aulas/agendamentos.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-05-S01]
    acceptanceCriteria:
      - Lembretes enviados no prazo.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-05-S04
    title: Cancelamentos
    description: Comunicação de cancelamentos.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-05-S01]
    acceptanceCriteria:
      - Cancelamentos comunicados.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-05-S05
    title: Cobranças
    description: Comunicação de cobranças.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-05-S01, EPIC-03-S01]
    acceptanceCriteria:
      - Cobranças comunicadas aos alunos.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-05-S06
    title: Templates
    description: Gestão de templates de mensagens.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-05-S01]
    acceptanceCriteria:
      - Templates configuráveis por tipo de mensagem.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 06 — Observabilidade

**Status:** TODO · **Priority:** MEDIUM · **Progress:** 0%

Métricas, health checks, logs estruturados e rastreamento.

```yaml
id: EPIC-06
title: Observabilidade
description: Métricas, health checks, logs estruturados e rastreamento.
status: TODO
priority: MEDIUM
progress: 0
dependsOn: [EPIC-01]
acceptanceCriteria:
  - Métricas, health checks e logs estruturados ativos.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-06-S01
    title: Micrometer
    description: Exposição de métricas via Micrometer.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Métricas Micrometer expostas.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-06-S02
    title: Prometheus
    description: Endpoint Prometheus de métricas.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-06-S01]
    acceptanceCriteria:
      - Endpoint Prometheus acessível.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-06-S03
    title: Health Checks
    description: Endpoint de health checks.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: []
    acceptanceCriteria:
      - Health checks disponíveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-06-S04
    title: Readiness
    description: Probe de readiness.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-06-S03]
    acceptanceCriteria:
      - Readiness funcional.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-06-S05
    title: Liveness
    description: Probe de liveness.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-06-S03]
    acceptanceCriteria:
      - Liveness funcional.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-06-S06
    title: TraceId
    description: Rastreamento com TraceId nas requisições.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: []
    acceptanceCriteria:
      - TraceId propagado nos logs.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-06-S07
    title: Logs estruturados
    description: Logs estruturados e consistentes.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-06-S06]
    acceptanceCriteria:
      - Logs estruturados e pesquisáveis.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 07 — DevOps

**Status:** TODO · **Priority:** MEDIUM · **Progress:** 0%

Containerização, pipelines, secrets e configuração por ambiente.

```yaml
id: EPIC-07
title: DevOps
description: Containerização, pipelines, secrets e configuração por ambiente.
status: TODO
priority: MEDIUM
progress: 0
dependsOn: [EPIC-06, EPIC-02]
acceptanceCriteria:
  - Build, deploy e configuração por ambiente automatizados.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-07-S01
    title: Dockerfile Backend
    description: Dockerfile otimizado para o backend.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Imagem construída com sucesso.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-07-S02
    title: Docker Compose Produção
    description: Docker Compose para produção.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-07-S01]
    acceptanceCriteria:
      - Stack de produção reproduzível via compose.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-07-S03
    title: Pipeline CI
    description: Pipeline de integração contínua.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-07-S01]
    acceptanceCriteria:
      - CI executa build e testes.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-07-S04
    title: Pipeline CD
    description: Pipeline de deploy contínuo.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-07-S03]
    acceptanceCriteria:
      - CD publica imagem e faz deploy.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-07-S05
    title: Scan Dependências
    description: Scan de dependências e vulnerabilities.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-07-S03]
    acceptanceCriteria:
      - Scan de dependências no pipeline.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-07-S06
    title: Secrets
    description: Gestão segura de secrets.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-07-S03]
    acceptanceCriteria:
      - Secrets fora do repositório.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-07-S07
    title: Configuração por ambiente
    description: Configuração por ambiente (dev, staging, produção).
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-07-S02]
    acceptanceCriteria:
      - Ambientes configurados corretamente.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 08 — Performance

**Status:** TODO · **Priority:** MEDIUM · **Progress:** 0%

Paginação, ordenação, filtros, índices e cache.

```yaml
id: EPIC-08
title: Performance
description: Paginação, ordenação, filtros, índices e cache.
status: TODO
priority: MEDIUM
progress: 0
dependsOn: [EPIC-01]
acceptanceCriteria:
  - Listagens paginadas, ordenadas, filtradas e com índices adequados.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-08-S01
    title: Paginação
    description: Paginação em listagens.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: []
    acceptanceCriteria:
      - Listagens paginadas.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-08-S02
    title: Ordenação
    description: Ordenação em listagens.
    status: TODO
    priority: MEDIUM
    estimate: S
    dependsOn: [EPIC-08-S01]
    acceptanceCriteria:
      - Ordenação definida de forma segura.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-08-S03
    title: Filtros
    description: Filtros em listagens.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-08-S01]
    acceptanceCriteria:
      - Filtros funcionais e seguros.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-08-S04
    title: Índices compostos
    description: Índices compostos para consultas frequentes.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-08-S01]
    acceptanceCriteria:
      - Índices cobrindo filtros frequentes.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
  - id: EPIC-08-S05
    title: Cache
    description: Cache onde fizer sentido.
    status: TODO
    priority: LOW
    estimate: L
    dependsOn: [EPIC-08-S01]
    acceptanceCriteria:
      - Cache aplicado em pontos de leitura frequente.
    definitionOfDone:
      - Implementação, testes e documentação atualizados.
    files: []
```

---

# EPIC 09 — Testes

**Status:** TODO · **Priority:** MEDIUM · **Progress:** 0%

Testes de integração, E2E, Testcontainers, multi-tenant e segurança.

```yaml
id: EPIC-09
title: Testes
description: Testes de integração, E2E, Testcontainers, multi-tenant e segurança.
status: TODO
priority: MEDIUM
progress: 0
dependsOn: [EPIC-01, EPIC-02]
acceptanceCriteria:
  - Suíte de testes de integração, E2E e segurança operacional.
definitionOfDone:
  - Implementação completa das stories abaixo.
  - Testes verdes.
  - Roadmap e PROJECT_STATUS atualizados.
stories:
  - id: EPIC-09-S01
    title: Integração
    description: Ampliar testes de integração.
    status: TODO
    priority: HIGH
    estimate: L
    dependsOn: []
    acceptanceCriteria:
      - Testes de integração cobrindo os domínios principais.
    definitionOfDone:
      - Testes verdes.
    files: []
  - id: EPIC-09-S02
    title: E2E Backend
    description: Testes E2E de backend.
    status: TODO
    priority: MEDIUM
    estimate: L
    dependsOn: [EPIC-09-S01]
    acceptanceCriteria:
      - Fluxos E2E validados.
    definitionOfDone:
      - Testes verdes.
    files: []
  - id: EPIC-09-S03
    title: Testcontainers
    description: Uso de Testcontainers para PostgreSQL.
    status: TODO
    priority: MEDIUM
    estimate: M
    dependsOn: [EPIC-09-S01]
    acceptanceCriteria:
      - Testes de integração com PostgreSQL real via Testcontainers.
    definitionOfDone:
      - Testes verdes.
    files: []
  - id: EPIC-09-S04
    title: Testes Multi-Tenant
    description: Testes de isolamento multi-tenant automatizados.
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-09-S01]
    acceptanceCriteria:
      - Isolamento entre studios validado por testes.
    definitionOfDone:
      - Testes verdes.
    files: []
  - id: EPIC-09-S05
    title: Testes de Segurança
    description: Testes de segurança (auth, RBAC, rate limiting, CORS).
    status: TODO
    priority: HIGH
    estimate: M
    dependsOn: [EPIC-09-S01]
    acceptanceCriteria:
      - Controles de segurança validados por testes.
    definitionOfDone:
      - Testes verdes.
    files: []
```

---

# 🟢 Antes do Go Live

- Pentest
- Backup
- Restore
- Monitoramento
- Documentação da API
- Manual Operacional
- Ambiente Staging
- Ambiente Produção

---

# Definição de Backend Pronto

- Todos os domínios implementados
- Multi-tenant validado
- Segurança conforme auditoria
- Financeiro completo
- Deploy automatizado
- Observabilidade ativa
- Cobertura de testes adequada
- Aprovado para operação com múltiplos studios
