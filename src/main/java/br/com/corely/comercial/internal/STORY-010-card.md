# STORY-010 — Payment (Liquidação de Invoice)

## Objetivo
Implementar o registro de pagamentos das Invoices. O Payment representa a liquidação financeira de uma Invoice. Uma Invoice pode possuir apenas um Payment.

## Escopo
- `PaymentMethod` enum (CASH, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, BOLETO, OTHER)
- `Payment` entity estendendo `ComercialBaseEntity`
- `PaymentRepository`, `PaymentService`, `PaymentController`
- DTOs (`PaymentRequest`, `PaymentResponse`)
- Migration V34
- Testes unitários e de integração

## Endpoints
```
POST /comercial/payments              (OWNER, ADMIN, FINANCIAL)
GET  /comercial/payments              (OWNER, ADMIN, FINANCIAL, RECEPTIONIST)
GET  /comercial/payments/{id}         (OWNER, ADMIN, FINANCIAL, RECEPTIONIST)
```

## Regras
- Todo Payment pertence a uma Invoice
- Uma Invoice pode possuir apenas um Payment (UNIQUE invoice_id)
- Apenas Invoices PENDING podem ser pagas
- Ao registrar: criar Payment + alterar Invoice para PAID (mesma transação)
- Valor do pagamento deve ser exatamente igual ao valor da Invoice
- Não permitir exclusão física
- Não permitir alteração após confirmação

## Fora do Escopo
- Estorno, Pagamento parcial, Múltiplos pagamentos
- Gateway de pagamento, PIX automático, Boleto, Cartão online
- Conciliação bancária, Frontend

## Dependências
- STORY-009 (Invoice)
