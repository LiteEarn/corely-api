# STORY-011 — Billing Schedule (Agenda de Cobrança)

## Objetivo
Implementar a agenda de cobrança dos contratos. Essa entidade define quando uma Invoice deve ser gerada para um StudentPlan. Nenhuma Invoice será criada automaticamente nesta história.

## Escopo
- `BillingFrequency` enum (WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL)
- `BillingSchedule` entity estendendo `ComercialBaseEntity`
- `BillingScheduleRepository`, `BillingScheduleService`, `BillingScheduleController`
- DTOs (`BillingScheduleRequest`, `BillingScheduleResponse`)
- Migration V35
- Testes unitários e de integração
- Swagger

## Endpoints
```
GET  /comercial/billing-schedules              (OWNER, ADMIN, FINANCIAL, RECEPTIONIST)
GET  /comercial/billing-schedules/{id}         (OWNER, ADMIN, FINANCIAL, RECEPTIONIST)
PUT  /comercial/billing-schedules/{id}         (OWNER, ADMIN, FINANCIAL)
```

## Regras
- Todo StudentPlan possui apenas um BillingSchedule
- Criar automaticamente junto com o StudentPlan
- billingDay deve estar entre 1 e 31
- nextBillingDate deve ser calculada automaticamente
- Apenas StudentPlans ACTIVE podem possuir BillingSchedule ativo
- Não permitir exclusão física
- Permitir ativação e inativação

## Fora do Escopo
- Geração automática de Invoice
- Scheduler, Jobs, Cobrança, Payment
- Notificações, Frontend

## Dependências
- STORY-008 (StudentPlan)
