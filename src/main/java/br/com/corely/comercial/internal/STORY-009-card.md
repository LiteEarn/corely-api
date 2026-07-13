# STORY-009 — Invoice (Faturamento)

## Objetivo
Implementar o faturamento do contrato do aluno. A Invoice representa um título financeiro gerado a partir de um StudentPlan. Nenhum pagamento será registrado nesta história.

## Escopo
- `InvoiceStatus` enum (PENDING, PAID, OVERDUE, CANCELLED)
- `Invoice` entity estendendo `ComercialBaseEntity`
- `InvoiceRepository`, `InvoiceService`, `InvoiceController`
- DTOs (`InvoiceRequest`, `InvoiceResponse`)
- Migration V33
- Testes unitários e de integração
- Swagger

## Entidade
- id, studentPlan, dueDate, referenceMonth, amount, status, issueDate, createdAt, updatedAt
- amount copiado do ContractSnapshot (nunca do Plan diretamente)

## Endpoints
```
POST   /comercial/invoices              (OWNER, ADMIN, FINANCIAL)
GET    /comercial/invoices              (OWNER, ADMIN, FINANCIAL, RECEPTIONIST)
GET    /comercial/invoices/{id}         (OWNER, ADMIN, FINANCIAL, RECEPTIONIST)
PUT    /comercial/invoices/{id}/cancel  (OWNER, ADMIN, FINANCIAL)
```

## Regras
- Invoice sempre pertence a um StudentPlan
- Valor copiado do ContractSnapshot (snapshot.getPlanPrice())
- Apenas StudentPlan ACTIVE pode gerar Invoice
- UNIQUE(student_plan_id, reference_month) — uma invoice por mês por contrato
- Não permitir exclusão física
- Não implementar recorrência automática

## Fora do Escopo
- Payment, PIX, Cartão, Boleto, Recorrência, Cobrança automática, Notificações, Frontend

## Dependências
- STORY-008 (StudentPlan)
- STORY-007 (ContractSnapshot)
