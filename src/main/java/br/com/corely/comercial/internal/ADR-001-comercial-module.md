# Módulo Comercial — Infraestrutura Base (STORY-001)

## Estrutura de Pastas

```
br.com.corely.comercial/
├── ComercialBaseEntity.java          # Base entity com studio_id + @Filter de tenant
├── contractsnapshot/
│   ├── ContractSnapshot.java             # Snapshot imutável de contratação de plano
│   ├── ContractSnapshotRepository.java
│   └── ContractSnapshotService.java      # Criação interna (sem endpoints públicos)
├── config/
│   ├── ComercialOpenApiGroupConfig.java  # Grupo Swagger para /comercial/**
│   └── ComercialWebMvcConfig.java        # Registro do TenantInterceptor
├── internal/
│   ├── ADR-001-comercial-module.md       # Este documento
│   ├── STORY-003-card.md                 # Card da STORY-003
│   ├── STORY-004-card.md                 # Card da STORY-004
│   ├── STORY-006-card.md                 # Card da STORY-006
│   ├── STORY-007-card.md                 # Card da STORY-007
│   ├── STORY-008-card.md                 # Card da STORY-008
│   ├── STORY-009-card.md                 # Card da STORY-009
│   └── STORY-010-card.md                 # Card da STORY-010
├── planrule/
│   ├── PlanRule.java                     # Associação entre Plan e RuleDefinition
│   ├── PlanRuleRepository.java
│   ├── PlanRuleService.java
│   ├── PlanRuleController.java
│   └── dto/
│       ├── PlanRuleRequest.java
│       └── PlanRuleResponse.java
├── rbac/
│   └── ComercialPermission.java          # Permissões RBAC reservadas ao módulo
├── ruleengine/
│   ├── RuleEngine.java                   # Motor de regras — orquestra carga e resolução
│   ├── RuleResult.java                   # Resultado tipado com getters por código
│   ├── RuleResolver.java                 # Conversão String → Java type via ValueType
│   └── RuleException.java                # Exceção para erros do motor
├── studentplan/
│   ├── StudentPlanStatus.java            # Enum: ACTIVE, SUSPENDED, CANCELLED, FINISHED
│   ├── StudentPlan.java                  # Contrato do aluno vinculado a ContractSnapshot
│   ├── StudentPlanRepository.java
│   ├── StudentPlanService.java           # Cria snapshot automaticamente na contratação
│   ├── StudentPlanController.java        # Endpoints em /comercial/student-plans
│   └── dto/
│       ├── StudentPlanRequest.java
│       └── StudentPlanResponse.java
├── tenant/
│   ├── ComercialTenantContext.java       # Resolução de studioId exclusivamente do JWT
│   ├── TenantInterceptor.java            # Habilita o @Filter de tenant por request
│   ├── TenantInterceptor.java            # Habilita o @Filter de tenant por request
│   └── TenantResolutionException.java    # Exceção para falha de resolução de tenant
├── invoice/
│   ├── InvoiceStatus.java                # Enum: PENDING, PAID, OVERDUE, CANCELLED
│   ├── Invoice.java                      # Título financeiro vinculado a StudentPlan
│   ├── InvoiceRepository.java
│   ├── InvoiceService.java               # Valor copiado do ContractSnapshot
│   ├── InvoiceController.java            # Endpoints em /comercial/invoices
│   └── dto/
│       ├── InvoiceRequest.java
│       └── InvoiceResponse.java
├── payment/
│   ├── PaymentMethod.java                # Enum: CASH, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, BOLETO, OTHER
│   ├── Payment.java                      # Liquidação de Invoice
│   ├── PaymentRepository.java
│   ├── PaymentService.java               # Altera Invoice para PAID ao registrar
│   ├── PaymentController.java            # Endpoints em /comercial/payments
│   └── dto/
│       ├── PaymentRequest.java
│       ├── PaymentMethodDto.java
│       └── PaymentResponse.java
```

## Convenções Adotadas

- **Pacote raiz**: `br.com.corely.comercial`
- **Organização**: por feature (subpacotes tenant/, config/, rbac/)
- **Persistência**: entidades devem estender `ComercialBaseEntity` para herdar:
  - Campos auditáveis (id, createdAt, updatedAt) de `BaseEntity`
  - Vínculo obrigatório com `Studio` (studio_id)
  - `@FilterDef` e `@Filter` para scoping automático de tenant
- **RBAC**: permissões específicas do módulo no enum `ComercialPermission`
- **OpenAPI**: endpoints prefixados com `/comercial/**` são agrupados no Swagger

## Isolamento Multi-Tenant

1. **Resolução**: `ComercialTenantContext.getCurrentStudioId()` obtém o studioId
   exclusivamente do JWT via `AuthenticationFacade` — nunca de parâmetros de
   requisição, cabeçalhos ou DTOs.
2. **Aplicação automática**: `TenantInterceptor` (HandlerInterceptor) habilita
   o `@Filter(name = "comercialTenantFilter")` no EntityManager a cada request
   para paths `/comercial/**`, filtrando automaticamente por `studio_id`.
3. **Entidades**: toda nova entidade do módulo deve estender `ComercialBaseEntity`
   para que o filtro de tenant seja aplicado.
4. **Fallback**: para contextos não-web (schedulers, filas), use
   `ComercialTenantContext.getCurrentStudioId()` e habilite o filtro manualmente:
   ```java
   session.enableFilter("comercialTenantFilter")
       .setParameter("studioId", tenantContext.getCurrentStudioId());
   ```

## RBAC

Permissões reservadas no enum `ComercialPermission`:

| Permissão | Descrição |
|---|---|
| COMMERCIAL_PLAN_READ | Visualizar planos comerciais |
| COMMERCIAL_PLAN_WRITE | Criar/editar planos comerciais |
| COMMERCIAL_STUDENT_PLAN_READ | Visualizar contratos de alunos |
| COMMERCIAL_STUDENT_PLAN_WRITE | Criar/editar contratos |
| COMMERCIAL_INVOICE_READ | Visualizar faturas |
| COMMERCIAL_INVOICE_WRITE | Criar/baixar faturas |
| COMMERCIAL_PAYMENT_READ | Visualizar pagamentos |
| COMMERCIAL_PAYMENT_WRITE | Registrar pagamentos |
| COMMERCIAL_DASHBOARD_VIEW | Visualizar dashboard financeiro |

A integração destas permissões com o sistema RBAC existente
(`Permission.java`, `RolePermissions.java`, `AuthorizationInterceptor`)
será feita na história de CRUD de Planos ou na de Frontend.

## Swagger

Grupo `comercial` no OpenAPI, visível em:
- Swagger UI: `/swagger-ui/index.html` (selecionar "Módulo Comercial")
- API docs: `/v3/api-docs/comercial`

## Histórias Concluídas

### STORY-001 — Infraestrutura Base
- `ComercialBaseEntity`, `TenantInterceptor`, `ComercialTenantContext`, `ComercialPermission`, Swagger group

### STORY-002 — Catálogo de Rule Definitions (Jul/2026)
- Pacote `br.com.corely.comercial.ruledefinition`
- Entidade `RuleDefinition` (NÃO estende `ComercialBaseEntity` — não possui `studio_id`)
- Enums `ValueType` e `Category`
- Repository, Service, Controller, DTOs
- Endpoints em `/comercial/rule-definitions`
- Apenas OWNER/ADMIN podem alterar; perfis de leitura consultam apenas regras ativas
- Endpoint administrativo `/admin/all` para listar todas (inclusive inativas)
- Sem exclusão física — apenas ativação/inativação
- Migration V27 com índices e constraints CHECK

### STORY-003 — CRUD de Planos (Jul/2026)
- Pacote `br.com.corely.comercial.plan`
- Entidade `Plan` (estende `ComercialBaseEntity` — isolamento automático por tenant)
- Repository, Service, Controller, DTOs (`PlanRequest`, `PlanResponse`)
- Endpoints em `/comercial/plans`
- Operações: Criar, Buscar por ID, Listar (paginado), Atualizar, Ativar, Inativar
- Filtros na listagem: `name` (LIKE), `active`
- Paginação via `Pageable` do Spring Data
- Validações: nome obrigatório, preço > 0, duração > 0
- Nome único por Studio (validação em serviço + constraint UNIQUE(studio_id, name) na migration V28)
- Sem exclusão física — apenas ativação/inativação (com idempotência)
- Version inicia em 1, incrementa a cada atualização
- Apenas OWNER/ADMIN podem criar/alterar/ativar/inativar
- RECEPTIONIST e FINANCIAL possuem apenas leitura
- Migration V25 (criação da tabela) + V28 (unique constraint)
- Índices em: studio_id, active, name

### STORY-004 — Configuração de Regras dos Planos (PlanRule) (Jul/2026)
- Pacote `br.com.corely.comercial.planrule`
- Entidade `PlanRule` (estende `ComercialBaseEntity` — isolamento automático por tenant)
- Repository, Service, Controller, DTOs (`PlanRuleRequest`, `PlanRuleResponse`)
- Endpoints em `/comercial/plans/{planId}/rules`
- Operações: Criar, Listar, Atualizar, Remover
- Combinação (plan_id, rule_definition_id) única (constraint UNIQUE na migration V29)
- Não permite associação com RuleDefinition inativa
- Não permite associação duplicada
- Apenas OWNER/ADMIN podem criar/alterar/remover
- RECEPTIONIST e FINANCIAL possuem apenas consulta
- Migration V29 com FKs, UNIQUE constraint e índices
- Nenhuma lógica do Rule Engine implementada

### STORY-006 — Rule Engine - Motor de Regras Comerciais (Jul/2026)
- Pacote `br.com.corely.comercial.ruleengine`
- `RuleEngine` — facade que recebe Plan/PlanId, carrega PlanRules + RuleDefinitions ativas e produz `RuleResult`
- `RuleResult` — getters tipados: `getInteger`, `getBoolean`, `getString`, `getDecimal`
- `RuleResolver` — conversão automática String → Java type com base no `ValueType` (BOOLEAN, INTEGER, DECIMAL, STRING, ENUM)
- `RuleException` — exceção para regras obrigatórias ausentes ou códigos inexistentes
- Durante a mesma chamada as regras são carregadas uma única vez (apenas as associadas ao plano, sem scan global)
- Nenhuma lógica de cobrança, presença, StudentPlan ou cache distribuído
- Sem migrations ou alterações estruturais

### STORY-007 — Snapshot Contratual dos Planos (Jul/2026)
- Pacote `br.com.corely.comercial.contractsnapshot`
- `ContractSnapshot` — entidade imutável com: studioId, planId, planVersion, planName, planDescription, planPrice, planDuration, rules (JSON)
- `ContractSnapshotService` — criação interna (sem endpoints públicos), usa `RuleEngine` para resolver regras e Jackson para serializar o JSON
- Migration V31 — tabela `comercial_contract_snapshots` com índices em studio_id, plan_id e created_at
- Snapshot nunca pode ser atualizado ou excluído (sem update/delete no service)
- Regras armazenadas como `Map<String, Object>` → JSON

### STORY-008 — StudentPlan (Contrato do Aluno) (Jul/2026)
- Pacote `br.com.corely.comercial.studentplan`
- Entidade `StudentPlan` (estende `ComercialBaseEntity`) com FK para `ContractSnapshot` (nunca referencia Plan diretamente)
- Enum `StudentPlanStatus`: ACTIVE, SUSPENDED, CANCELLED, FINISHED
- A contratação cria automaticamente um `ContractSnapshot` via `ContractSnapshotService.create(planId)`
- Não permite dois contratos ACTIVE para o mesmo aluno (constraint UNIQUE + validação em serviço)
- Cancelamento preserva histórico (não remove registros)
- Suspend/Reactivate para pausar e retomar contratos
- Migration V32 com FKs para student e contract_snapshot, índices em student_id, status, start_date
- Endpoints: POST, GET (lista e por id), PUT cancel/suspend/reactivate
- RBAC: OWNER/ADMIN (total), RECEPTIONIST (criar/consultar), FINANCIAL (consultar)

### STORY-009 — Invoice (Faturamento) (Jul/2026)
- Pacote `br.com.corely.comercial.invoice`
- Entidade `Invoice` (estende `ComercialBaseEntity`) com FK para `StudentPlan`
- Enum `InvoiceStatus`: PENDING, PAID, OVERDUE, CANCELLED
- Valor copiado do `ContractSnapshot.getPlanPrice()` — nunca do Plan diretamente
- Apenas StudentPlan ACTIVE pode gerar Invoice
- UNIQUE(student_plan_id, reference_month) — uma invoice por mês por contrato
- Não permite exclusão física
- Migration V33 com FKs para student_plan, índices em student_plan_id, due_date, status, reference_month
- Endpoints: POST, GET (lista e por id), PUT cancel
- RBAC: OWNER/ADMIN/FINANCIAL (criar/consultar/cancelar), RECEPTIONIST (apenas consulta)
- Sem recorrência automática ou pagamento

### STORY-010 — Payment (Liquidação de Invoice) (Jul/2026)
- Pacote `br.com.corely.comercial.payment`
- Entidade `Payment` (estende `ComercialBaseEntity`) com FK única para `Invoice`
- Enum `PaymentMethod`: CASH, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, BOLETO, OTHER
- Ao registrar: cria Payment + altera Invoice para PAID (mesma transação)
- Apenas Invoices PENDING podem ser pagas — valida status antes do duplicado
- Valor do pagamento deve ser exatamente igual ao valor da Invoice
- UNIQUE(invoice_id) — uma Invoice pode possuir apenas um Payment
- Migration V34 com FKs, UNIQUE e índices
- Endpoints: POST, GET (lista e por id)
- RBAC: OWNER/ADMIN/FINANCIAL (criar/consultar), RECEPTIONIST (apenas consulta)

## Histórias Futuras (Roadmap)

1. Frontend — Telas do módulo
2. Dashboard Financeiro
