# STORY-028 — Cobranças (Invoices)

## Objetivo

Implementar o módulo responsável por controlar cobranças financeiras dos alunos.

Esta história não implementa pagamento.

Ela apenas controla as cobranças.

## Entidade

`br.com.corely.finance.invoice.Invoice`

Campos:
- id (UUID, herdado)
- student (ManyToOne → Student)
- studio (herdado de ComercialBaseEntity)
- dueDate (LocalDate)
- amount (BigDecimal)
- description (String, opcional)
- status (InvoiceStatus: PENDING, PAID, OVERDUE, CANCELLED)
- paymentDate (LocalDate, nullable)
- createdAt (herdado)
- updatedAt (herdado)

## Endpoints

`GET /finance/invoices`
`GET /finance/invoices/{id}`
`POST /finance/invoices`
`POST /finance/invoices/{id}/pay`
`POST /finance/invoices/{id}/cancel`

## Regras de Negócio

- Não permitir cancelar cobrança paga
- Não permitir pagar cobrança cancelada
- Não permitir pagar duas vezes
- Ao marcar como paga: alterar status para PAID e preencher paymentDate com LocalDate.now()

## Multi-tenant

Obrigatório — entidade estende ComercialBaseEntity, com @Filter("comercialTenantFilter").
TenantInterceptor ativado para paths /finance/**.

## Segurança

- OWNER, ADMIN, FINANCIAL: criação, consulta, pagamento e cancelamento
- RECEPTIONIST: apenas consulta (GET)

## Swagger

Grupo "Módulo Financeiro" em `/finance/**`.

## Migration

V47__create_finance_invoices.sql com índices em student_id, status, due_date.

## Testes

- Unitários (InvoiceServiceTest)
- Controller (InvoiceControllerTest)
- Tenant Isolation (TenantIsolationTest)
