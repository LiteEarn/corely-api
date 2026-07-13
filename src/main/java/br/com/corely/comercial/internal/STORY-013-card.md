# STORY-013 — Processamento de Inadimplência (Overdue Processing)

## Objetivo
Implementar o processamento automático de Invoices vencidas. Invoices com dueDate anterior à data de processamento e status PENDING devem ser marcadas como OVERDUE.

## Escopo
- `OverdueProcessingResult` — DTO com processed, overdue, skipped, errors
- `OverdueProcessingService` — serviço interno (sem endpoint público)
- Testes unitários e de integração
- Documentação

## Regras
- Buscar Invoices com status PENDING e dueDate < data informada
- Alterar status para OVERDUE
- Nunca alterar Invoice PAID, CANCELLED ou já OVERDUE
- Processar cada Invoice em transação independente
- Erro em uma Invoice não interrompe as demais

## Fora do Escopo
- Suspensão automática do StudentPlan
- Notificações, WhatsApp, E-mail
- Dashboard, Frontend

## Dependências
- STORY-009 (Invoice)
