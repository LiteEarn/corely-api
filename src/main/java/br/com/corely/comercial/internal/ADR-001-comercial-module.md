# Módulo Comercial — Infraestrutura Base (STORY-001)

## Estrutura de Pastas

```
br.com.corely.comercial/
├── ComercialBaseEntity.java          # Base entity com studio_id + @Filter de tenant
├── config/
│   ├── ComercialOpenApiGroupConfig.java  # Grupo Swagger para /comercial/**
│   └── ComercialWebMvcConfig.java        # Registro do TenantInterceptor
├── internal/
│   └── ADR-001-comercial-module.md       # Este documento
├── rbac/
│   └── ComercialPermission.java          # Permissões RBAC reservadas ao módulo
└── tenant/
    ├── ComercialTenantContext.java       # Resolução de studioId exclusivamente do JWT
    ├── TenantInterceptor.java            # Habilita o @Filter de tenant por request
    └── TenantResolutionException.java    # Exceção para falha de resolução de tenant
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

## Histórias Futuras (Roadmap)

1. Rule Engine — Motor de regras configurável
2. CRUD de Planos — Entidade Plan
3. Frontend — Telas do módulo
4. StudentPlan — Contratos de alunos
5. Invoice — Faturamento
6. Payment — Pagamentos
7. Dashboard Financeiro
