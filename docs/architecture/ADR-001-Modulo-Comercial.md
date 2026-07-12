# ADR-001 --- Arquitetura do Módulo Comercial do Corely

**Status:** Aprovado para implementação\
**Versão:** 1.0\
**Data:** 12/07/2026

## 1. Objetivo

Definir a arquitetura oficial do módulo comercial do Corely.

O Corely não impõe um modelo comercial aos Studios. Ele fornece uma
plataforma configurável baseada em regras.

## 2. Visão Arquitetural

Produto Comercial ↓ Contrato do Aluno ↓ Motor de Regras ↓ Operação ↓
Financeiro ↓ Integrações

Cada domínio possui responsabilidade única.

## 3. Princípios

-   Configuração ao invés de customização
-   Baixo acoplamento
-   Alta coesão
-   Open/Closed Principle
-   Domain Driven Design
-   Multi-tenant
-   Rule Engine como núcleo do domínio

## 4. Modelo de Domínio

### Plan

Produto comercial reutilizável.

### StudentPlan

Contrato do aluno contendo snapshot do plano contratado.

### Enrollment

Vínculo operacional.

### Attendance

Registro de presença.

### Invoice

Conta a receber.

### Payment

Recebimento financeiro.

## 5. Decisões Arquiteturais

-   Não existe PlanType.
-   O comportamento do plano é determinado exclusivamente pelas regras.
-   RuleDefinition pertence ao Corely.
-   Studios apenas configuram PlanRules.
-   Créditos são uma estratégia, não uma entidade obrigatória.
-   Planos são versionados.
-   StudentPlan mantém snapshot contratual.
-   Toda decisão comercial passa pelo Rule Engine.

## 6. Multi-tenancy

-   studioId obtido exclusivamente do JWT.
-   Nunca recebido do frontend.
-   Nunca presente em DTOs.
-   Toda consulta filtrada automaticamente pelo tenant.

## 7. Roadmap

ADR → Rule Engine → RuleDefinition → CRUD de Planos → Frontend →
StudentPlan → Invoice → Payment → Dashboard Financeiro

## 8. Conclusão

O Corely é uma plataforma comercial configurável. Os Studios montam seus
produtos através de regras, preservando flexibilidade e escalabilidade.
