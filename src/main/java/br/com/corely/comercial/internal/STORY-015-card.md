# STORY-015 — Processador de Inadimplência (Delinquency Processor)

## Objetivo
Executar a política de inadimplência configurada para cada Studio. Quando um contrato tem faturas OVERDUE que excederam o grace period, a ação configurada (SUSPEND_CONTRACT ou BLOCK_NEW_BOOKINGS) deve ser aplicada.

## Escopo
- `DelinquencyProcessorResult` — DTO com contadores processed/suspended/blocked/skipped/errors
- `DelinquencyProcessorService` — serviço interno (sem endpoint público)
- Testes unitários e de integração
- Documentação

## Funcionamento
Receber uma data de processamento.

Buscar StudentPlans com:
- status ACTIVE
- possui fatura OVERDUE
- fatura mais antiga OVERDUE excedeu grace period da DelinquencyPolicy

Para cada StudentPlan:
- validar se já não está suspenso
- aplicar ação da DelinquencyPolicy:
  - SUSPEND_CONTRACT → suspender StudentPlan
  - BLOCK_NEW_BOOKINGS → registrar bloqueio (sem implementação de bloqueio real)
  - NONE → não fazer nada
- registrar no resultado

## Regras
- Processar cada contrato em transação independente
- Erro em um contrato não interrompe os demais
- Não alterar StudentPlans já SUSPENDED/CANCELLED/FINISHED
- Respeitar gracePeriodDays da política

## Fora do Escopo
- Bloqueio real de agendamento (BLOCK_NEW_BOOKINGS)
- Suspensão automática por scheduler
- Notificações
- Frontend

## Dependências
- STORY-013 (Overdue Processing)
- STORY-014 (Delinquency Policy)