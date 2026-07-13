# STORY-007 — Snapshot Contratual dos Planos

## Objetivo
Implementar o mecanismo de Snapshot Contratual. Sempre que um aluno contratar um plano, todas as informações comerciais deverão ser copiadas para um snapshot imutável. Alterações futuras no plano não podem impactar contratos já existentes.

## Escopo
- `ContractSnapshot` — entidade JPA com planId, planVersion, planName, planDescription, planPrice, planDuration, rules (JSON), createdAt
- `ContractSnapshotRepository` — com índices em plan_id e created_at
- `ContractSnapshotService` — criação interna (sem endpoint público)
- Migration V31 — tabela `comercial_contract_snapshots`
- Testes unitários e de integração

## Regras
- Snapshot imutável: nunca pode ser atualizado ou excluído
- Deve copiar todas as PlanRules resolvidas via RuleEngine
- Deve armazenar os valores já convertidos em JSON
- O JSON representa exatamente as regras vigentes no momento da contratação

## Fora do Escopo
- StudentPlan, Cobrança, Invoice, Payment, Frontend
- Endpoints públicos para snapshots

## Dependências
- STORY-002 (RuleDefinition)
- STORY-003 (Plan)
- STORY-004 (PlanRule)
- STORY-006 (RuleEngine)
