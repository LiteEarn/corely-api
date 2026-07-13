# STORY-006 — Rule Engine - Motor de Regras Comerciais

## Objetivo
Implementar a primeira versão do Rule Engine responsável por interpretar as RuleDefinitions configuradas em um Plan. O motor valida e disponibiliza os valores das regras com conversão automática de tipos.

## Escopo
- `RuleException` — exceção para erros do motor
- `RuleResolver` — conversão de String para tipos Java (BOOLEAN, INTEGER, DECIMAL, STRING, ENUM)
- `RuleResult` — objeto com getters tipados (getInteger, getBoolean, getString, getDecimal)
- `RuleEngine` — facade que orquestra carga de PlanRules + RuleDefinitions e produz RuleResult
- Testes unitários (mock) e de integração (Spring Boot + H2)
- Documentação atualizada

## Fora do Escopo
- StudentPlan, Cobrança, Invoice, Payment, Attendance, Renovação
- Snapshot ou versionamento funcional
- Cache distribuído
- Frontend

## Critérios de Aceite
- RuleEngine recebe PlanId ou Plan e retorna RuleResult
- RuleResult resolve valores por código com tipo correto
- Regra obrigatória ausente lança RuleException
- Regra opcional ausente retorna defaultValue da RuleDefinition
- Conversão automática: BOOLEAN, INTEGER, DECIMAL, STRING, ENUM
- Nenhuma lógica de cobrança ou presença
- Testes automatizados passando
- Documentação atualizada no ADR-001

## Dependências
- STORY-002 (RuleDefinition, ValueType, Category)
- STORY-003 (Plan)
- STORY-004 (PlanRule)
- STORY-005 (Seed de RuleDefinitions — opcional, mas desejável para testes)
