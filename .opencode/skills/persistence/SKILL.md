---
name: persistence
description: Conhecimento reutilizavel de persistencia no Corely (JPA, Hibernate, EntityGraph, batch queries, Flyway, evitar N+1). Use ao implementar ou revisar repositories, entidades e migrations.
---

# Persistence

Conhecimento reutilizavel de persistencia no Corely (Spring Data JPA / Hibernate).

## Entidades e relacoes

- `@ManyToOne` e `@OneToOne` associativas: usar `FetchType.LAZY` para evitar cargas desnecessarias.
- Associacoes unidirecionais simples quando nao houver necessidade de navegacao inversa.
- Preferir IDs `UUID`.

## Evitar N+1

- Usar `@EntityGraph(attributePaths = {...})` nas consultas que precisam de associacoes eager.
- Para listas de filhos relacionados (ex.: billing schedules por student plan), usar **batch query** (`findByXIn(...)`) e montar um mapa em memoria.
- **Nunca** disparar consultas adicionais dentro do mapper/Service de forma implicita.

## Repositories

- `JpaRepository<T, UUID>` com metodos derivados de nome quando possivel.
- Consultas JPQL com `@Query` quando a query for complexa; sempre incluir filtro multi-tenant.
- Retornar `Optional<T>` quando a ausencia e possivel.

## Flyway

- Migrations em `src/main/resources/db/migration/V<n>__<descricao>.sql` com prefixo de schema `corely.`.
- Nunca alterar tabela existente sem nova migration.
- Datasource com `currentSchema=corely`.

## Convencoes

- `ContractSnapshot` armazena regras como JSON; a interpretacao e feita exclusivamente por `ContractSnapshotParser` (nunca em Service/Mapper/Controller).
- Atualizar entidade retornada pelo `save()` (o retorno pode ser a instancia gerenciada).
