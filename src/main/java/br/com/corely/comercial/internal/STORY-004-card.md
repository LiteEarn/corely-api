# STORY-004 — Configuração de Regras dos Planos (PlanRule)

**Status:** Em Review
**Módulo:** Comercial
**Dependências:** STORY-002 (Rule Definitions), STORY-003 (CRUD de Planos)

## Objetivo

Permitir que um Plano seja configurado através de RuleDefinitions.

Apenas a associação entre Plan e RuleDefinition. Nenhuma regra será executada nesta etapa.

## Entidade PlanRule

- `id` (UUID, gerado automaticamente)
- `plan` (FK → comercial_plans)
- `ruleDefinition` (FK → comercial_rule_definitions)
- `value` (String, armazenado como VARCHAR)
- `createdAt`, `updatedAt` (auditoria via BaseEntity)

Herda de `ComercialBaseEntity` → isolamento automático por tenant via `@Filter`.

## Regras

- Um Plano pode possuir várias RuleDefinitions
- Uma RuleDefinition pode ser utilizada em vários Planos
- Combinação (plan_id, rule_definition_id) deve ser única
- Não permitir RuleDefinition inativa
- Não permitir associação duplicada
- Campo value armazenado como String
- Nenhuma lógica do Rule Engine implementada

## Endpoints

| Método | Path | Roles | Descrição |
|--------|------|-------|-----------|
| POST | `/comercial/plans/{planId}/rules` | OWNER, ADMIN | Associar regra ao plano |
| GET | `/comercial/plans/{planId}/rules` | OWNER, ADMIN, RECEPTIONIST, FINANCIAL | Listar regras do plano |
| PUT | `/comercial/plans/{planId}/rules/{ruleId}` | OWNER, ADMIN | Atualizar regra do plano |
| DELETE | `/comercial/plans/{planId}/rules/{ruleId}` | OWNER, ADMIN | Remover regra do plano |

## Segurança

- OWNER e ADMIN: escrita (criar, alterar, remover)
- RECEPTIONIST e FINANCIAL: apenas consulta

## Fora do Escopo

- Rule Engine
- Validação de value conforme ValueType
- StudentPlan
- Snapshot
- Versionamento funcional
- Frontend

## Arquivos Criados

- `db/migration/V29__create_comercial_plan_rules.sql`
- `planrule/PlanRule.java`
- `planrule/PlanRuleRepository.java`
- `planrule/PlanRuleService.java`
- `planrule/PlanRuleController.java`
- `planrule/dto/PlanRuleRequest.java`
- `planrule/dto/PlanRuleResponse.java`
- `planrule/PlanRuleServiceTest.java`
- `planrule/PlanRuleControllerTest.java`
- `internal/STORY-004-card.md`

## Arquivos Modificados

- `internal/ADR-001-comercial-module.md`
