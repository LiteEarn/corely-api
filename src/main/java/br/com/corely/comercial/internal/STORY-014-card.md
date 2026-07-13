# STORY-014 — Política de Inadimplência do Studio

## Objetivo
Permitir que cada Studio configure sua política de inadimplência. O sistema ainda NÃO executará bloqueios automaticamente.

## Escopo
- `DelinquencyAction` enum (NONE, BLOCK_NEW_BOOKINGS, SUSPEND_CONTRACT)
- `DelinquencyPolicy` entity estendendo `ComercialBaseEntity`
- `DelinquencyPolicyRepository`, `DelinquencyPolicyService`, `DelinquencyPolicyController`
- DTOs (`DelinquencyPolicyRequest`, `DelinquencyPolicyResponse`)
- Migration V36
- Testes unitários e de integração
- Swagger

## Endpoints
```
GET  /comercial/delinquency-policy        (OWNER, ADMIN, FINANCIAL)
PUT  /comercial/delinquency-policy        (OWNER, ADMIN)
```

## Regras
- Cada Studio possui apenas uma política (UNIQUE studio_id)
- Criar automaticamente na inicialização
- gracePeriodDays >= 0
- Não permitir exclusão física
- Sempre deve existir uma política

## Fora do Escopo
- Suspensão automática, Bloqueio de agenda
- Notificações, WhatsApp, Frontend
