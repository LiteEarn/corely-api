# STORY-003 — CRUD de Planos

**Status:** Em Review  
**Módulo:** Comercial  
**Dependências:** STORY-001 (Infraestrutura Base)

## Objetivo

Implementar o CRUD completo da entidade Plan.

## Entidade Plan

- `id` (UUID, gerado automaticamente)
- `studio` (FK → studios, via ComercialBaseEntity)
- `name` (único por Studio)
- `description` (TEXT, opcional)
- `price` (Decimal, > 0)
- `duration` (Integer, > 0)
- `version` (Integer, inicia em 1)
- `active` (Boolean, default true)
- `createdAt`, `updatedAt` (auditoria via BaseEntity)

Herda de `ComercialBaseEntity` → isolamento automático por tenant via `@Filter`.

## Regras

- Nome único por Studio (constraint UNIQUE(studio_id, name))
- Preço > 0
- Duração > 0
- Version inicia em 1, incrementa a cada update
- Sem exclusão física (apenas ativação/inativação)
- Ativação/inativação com idempotência (lança BusinessException se já estiver no estado solicitado)

## Endpoints

| Método | Path | Roles | Descrição |
|--------|------|-------|-----------|
| POST | `/comercial/plans` | OWNER, ADMIN | Criar plano |
| GET | `/comercial/plans` | OWNER, ADMIN, RECEPTIONIST, FINANCIAL | Listar planos (paginado, filtros: name, active) |
| GET | `/comercial/plans/{id}` | OWNER, ADMIN, RECEPTIONIST, FINANCIAL | Buscar por ID |
| PUT | `/comercial/plans/{id}` | OWNER, ADMIN | Atualizar plano |
| POST | `/comercial/plans/{id}/activate` | OWNER, ADMIN | Ativar plano |
| POST | `/comercial/plans/{id}/inactivate` | OWNER, ADMIN | Inativar plano |

## Segurança

- OWNER e ADMIN: escrita (criar, alterar, ativar, inativar)
- RECEPTIONIST e FINANCIAL: apenas leitura (listar, buscar por id)
- Nenhum endpoint recebe `studioId` — isolamento via `ComercialTenantContext`

## Fora do Escopo

- PlanRule / Rule Engine
- StudentPlan
- Frontend
- Versionamento funcional
- Snapshot contratual

## Arquivos Alterados/Criados

### Criados
- `db/migration/V28__add_unique_constraint_comercial_plans.sql`
- `plan/PlanServiceTest.java`
- `plan/PlanControllerTest.java`
- `internal/STORY-003-card.md`

### Modificados
- `plan/PlanRepository.java`
- `plan/PlanService.java`
- `plan/PlanController.java`
- `plan/TenantIsolationTest.java`
- `internal/ADR-001-comercial-module.md`
