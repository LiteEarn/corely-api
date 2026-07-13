# STORY-012 — Geração Automática de Invoices

## Objetivo
Implementar o processo automático de geração de Invoices a partir dos BillingSchedules. A execução será por serviço interno, sem scheduler.

## Escopo
- `InvoiceGenerationResult` — DTO com campos processed, generated, skipped, errors
- `InvoiceGenerationService` — serviço interno que processa BillingSchedules vencidos
- Testes unitários e de integração
- Documentação

## Regras
- Buscar BillingSchedules ativos com nextBillingDate <= data informada
- Validar StudentPlan ACTIVE antes de gerar
- Verificar duplicidade por student_plan_id + reference_month
- Criar Invoice com valor do ContractSnapshot
- Atualizar nextBillingDate após cada geração
- Erro em um contrato não interrompe os demais
- Nenhum endpoint público

## Fora do Escopo
- @Scheduled, Quartz, Spring Batch
- PIX, Boleto, Cartão
- Notificações, Frontend

## Dependências
- STORY-009 (Invoice)
- STORY-011 (BillingSchedule)
