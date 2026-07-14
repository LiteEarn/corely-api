# STORY-018 — Encerramento Automático de Contratos

## Objetivo
Finalizar automaticamente contratos que chegaram ao fim da vigência e não são elegíveis para renovação automática.

## Escopo
- `ContractExpirationResult` — DTO com contadores processed/finished/skipped/errors
- `ContractExpirationService` — serviço interno (sem endpoint público)
- Método `deactivateSchedule` no `BillingScheduleService`
- Query `findByStatusAndEndDateBefore` no `StudentPlanRepository`
- Testes unitários e de integração
- Documentação

## Funcionamento
Receber uma data de processamento.

Buscar StudentPlans:
- status = ACTIVE
- endDate < processingDate

Para cada contrato:
- localizar o plano
- verificar autoRenew
- se autoRenew = true, ignorar
- se autoRenew = false:
  - alterar status para FINISHED
  - desativar BillingSchedule
  - remover bookingBlocked
  - registrar resultado

## Regras
- Nunca finalizar contratos CANCELLED
- Nunca finalizar contratos SUSPENDED
- Nunca finalizar contratos já FINISHED
- Processar cada contrato em transação independente (TransactionTemplate)
- Erro em um contrato não interrompe os demais

## Banco
Nenhuma migration.

## API
Não criar endpoint público.

Será utilizado futuramente por Scheduler.

## Fora do Escopo
- Notificações
- WhatsApp
- Dashboard
- Frontend

## Dependências
- STORY-003 (Plan) — consulta de autoRenew
- STORY-008 (StudentPlan) — contratos com status e datas
- STORY-011 (BillingSchedule) — desativação de agenda de cobrança
- STORY-017 (ContractRenewal) — complementar: expiração lida com planos sem autoRenew
