# STORY-016 — Reativação Automática de Contratos

## Objetivo
Reativar automaticamente contratos suspensos por inadimplência quando não existirem mais Invoices em atraso.

## Escopo
- `ContractReactivationResult` — DTO com contadores processed/reactivated/skipped/errors
- `ContractReactivationService` — serviço interno (sem endpoint público)
- Testes unitários e de integração
- Documentação

## Funcionamento
Receber uma data de processamento.

Buscar StudentPlans com status SUSPENDED.

Para cada contrato:
- verificar se ainda existe Invoice OVERDUE
- se existir, não fazer nada (skip)
- se não existir:
  - alterar status para ACTIVE
  - remover bookingBlocked (false)
  - persistir alteração

## Regras
- Processar cada contrato em transação independente (TransactionTemplate)
- Erro em um contrato não interrompe os demais
- Nunca reativar contratos CANCELLED ou FINISHED
- Reativar apenas contratos SUSPENDED

## Fora do Escopo
- Notificações
- WhatsApp
- E-mail
- Dashboard
- Frontend

## Dependências
- STORY-015 (Delinquency Processor) — cria bookingBlocked e StudentPlan.SUSPENDED
