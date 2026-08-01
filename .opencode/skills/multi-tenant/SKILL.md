---
name: multi-tenant
description: Conhecimento reutilizavel de multi-tenant no Corely (isolamento por studioId, filtros Hibernate, contexto de tenant). Use ao implementar ou revisar qualquer consulta ou entidade.
---

# Multi-Tenant

Conhecimento reutilizavel de multi-tenant no Corely.

## Principio

Todo dado pertence a um **Studio**. Nunca permitir acesso entre studios.

## Mecanismo

- **Contexto global**: `br.com.corely.shared.tenant.TenantContext` resolve o
  `studioId` corrente exclusivamente a partir do contexto de autenticação
  (`TenantContext.getCurrentStudioId()`). É a fonte única reutilizada pelos
  filtros e Services. `ComercialTenantContext` delega para ele (compatibilidade).
- Entidades do modulo Comercial extendem `ComercialBaseEntity`, que possui `studio` (ManyToOne) e o `@FilterDef("comercialTenantFilter")`.
- `@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")` aplicado nas entidades.
- `TenantInterceptor` habilita o filtro de tenant por requisição usando o `TenantContext` global.

## Regras

- **Nunca** executar `findAll()` sem filtro de tenant quando a entidade for multi-tenant.
- Ao adicionar consultas JPQL, garantir que o filtro ou a condicao `studio_id` seja aplicado.
- DTOs nao devem receber `studioId` controlado pelo client.
- Testes de isolamento: validar que um studio nao acessa dados de outro.
- Ao criar entidades, sempre setar o `studio` correto.

## Revisao

- Verificar ausencia de consultas que ignorem `studio_id`.
- Verificar que o EntityGraph e o filtro coexistam corretamente.
