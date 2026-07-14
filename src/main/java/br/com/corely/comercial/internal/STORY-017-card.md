# STORY-017 — Renovação Automática de Contratos

## Objetivo
Renovar automaticamente contratos elegíveis conforme as regras do plano.

## Escopo
- `ContractRenewalResult` — DTO com contadores processed/renewed/skipped/errors
- `ContractRenewalService` — serviço interno (sem endpoint público)
- Migration adicionando `auto_renew` ao Plan
- Testes unitários e de integração
- Documentação

## Funcionamento
Receber uma data de processamento.

Buscar StudentPlans:
- status = ACTIVE
- endDate <= processingDate

Para cada contrato:
- verificar se o plano permite renovação automática (auto_renew = true)
- verificar se não existem Invoices OVERDUE
- renovar o contrato:
  - recalcular endDate (startDate + duration)
  - gerar novo ContractSnapshot
  - criar novo BillingSchedule quando necessário

## Regras
- Nunca renovar contratos CANCELLED
- Nunca renovar contratos FINISHED
- Nunca renovar contratos SUSPENDED
- Nunca renovar contratos com Invoice OVERDUE
- Processar cada contrato em transação independente (TransactionTemplate)
- Erro em um contrato não interrompe os demais

## Banco
Migration V39 adicionando ao Plan:
- `auto_renew BOOLEAN NOT NULL DEFAULT TRUE`

## API
Não criar endpoint público.

Será utilizado futuramente por Scheduler.

## Fora do Escopo
- Notificações
- WhatsApp
- PIX
- Boleto
- Dashboard
- Frontend

## Dependências
- STORY-007 (ContractSnapshot) — criação de snapshots
- STORY-011 (BillingSchedule) — recriação de agenda de cobrança
