# STORY-005 — Seed Oficial de RuleDefinitions

**Status:** Em Desenvolvimento
**Módulo:** Comercial
**Dependências:** STORY-002 (Rule Definitions)

## Objetivo

Popular o catálogo oficial de RuleDefinitions do Corely com as regras nativas da plataforma.

Apenas o Corely disponibiliza novas RuleDefinitions através de novas versões da aplicação. Studios não podem criar RuleDefinitions.

## RuleDefinitions

### VALIDITY
- **VALIDITY_DAYS** (INTEGER) — Dias de vigência do plano

### ATTENDANCE
- **MAX_CLASSES** (INTEGER) — Máximo de aulas por ciclo

### BOOKING
- **MAX_FUTURE_BOOKINGS** (INTEGER) — Máximo de agendamentos futuros
- **DAILY_LIMIT** (INTEGER) — Limite diário de agendamentos

### CANCELLATION
- **ALLOW_MAKEUP** (BOOLEAN) — Permitir reposição
- **MAKEUP_VALIDITY_DAYS** (INTEGER) — Dias para usar reposição

### BILLING
- **AUTO_RENEW** (BOOLEAN) — Renovação automática
- **BILLING_CYCLE** (STRING) — Ciclo de cobrança
- **GRACE_PERIOD_DAYS** (INTEGER) — Dias de tolerância

### GENERAL
- **ACTIVE_ON_PAYMENT** (BOOLEAN) — Ativar mediante pagamento
- **ALLOW_OVERDUE_BOOKING** (BOOLEAN) — Permitir agendamento em atraso

## Regras

- Todos os códigos únicos (constraint UNIQUE na tabela)
- Todas iniciam ativas
- Migration idempotente (INSERT com WHERE NOT EXISTS)
- Nenhum endpoint novo ou alteração de API

## Fora do Escopo

- Rule Engine
- Validação de RuleValue
- StudentPlan
- Snapshot / Versionamento
- Frontend

## Arquivos Criados

- `db/migration/V30__seed_comercial_rule_definitions.sql`
- `ruledefinition/RuleDefinitionSeedTest.java`
- `internal/STORY-005-card.md`

## Arquivos Modificados

- `internal/ADR-001-comercial-module.md`
