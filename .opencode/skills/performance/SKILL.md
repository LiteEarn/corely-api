---
name: performance
description: Conhecimento reutilizavel de performance no Corely (N+1, EntityGraph, batch queries, indices, paginacao, cache). Use ao revisar ou otimizar consultas e endpoints.
---

# Performance

Conhecimento reutilizavel de performance no Corely.

## Consultas

- **N+1**: evitar carregando associacoes com `@EntityGraph` ou `JOIN FETCH`.
- **Batch queries**: para listas de relacionamentos 1-N, buscar todos de uma vez (`findByXIn(...)`) e montar mapa em memoria, evitando query por elemento.
- **Consultas ocultas**: garantir que mappers/`toResponse` nao disparem queries adicionais (usar dados ja carregados).
- Evitar carregar entidades inteiras quando so o ID e necessario (`getReferenceById`).

## Indices

- Indices compostos para filtros frequentes.
- Verificar planos de execucao para queries complexas.

## Paginacao e ordenacao

- Usar paginacao para listas potencialmente grandes (roadmap EPIC 08).
- Ordenacao e filtros definidos de forma segura (sem SQL injection).

## Cache

- Cache apenas onde fizer sentido (roadmap EPIC 08).

## Revisao de performance

1. Identificar loops que chamam repository.
2. Verificar `@EntityGraph` presente em consultas que acessam associacoes.
3. Confirmar ausencia de lazy loading fora do contexto transacional.
4. Confirmar batch lookup para relacionamentos em listas.
