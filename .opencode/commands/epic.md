---
description: Implementa um epico do roadmap e entrega para a aprovacao do @corely-review. Ex.: /epic 03
agent: corely-dev
---

Voce e o agente **corely-dev**. Implemente o epico **$ARGUMENTS** do roadmap do Corely e entregue para a **aprovacao do @corely-review**.

## Passos

1. **Localizar o epico**: leia `Corely_Backend_Roadmap_GoLive.md` e encontre o epico de numero **$ARGUMENTS** (ex.: "EPIC 03"). Verifique tambem se existe um documento detalhado em `docs/epico/` (ex.: `EpicAgendaOperacional.md`).
2. **Analisar codigo**: explore o codigo existente dos modulos relacionados para entender o que ja foi implementado.
3. **Verificar dependencias**: confirme que os modulos/entidades dependentes existem.
4. **Implementar apenas o que falta**: implemente somente o escopo do epico, sem adicionar funcionalidades fora dele. Marque as stories como `IN_PROGRESS` durante o trabalho.
5. **Criar testes**: adicione/atualize testes unitarios e de integracao.
6. **Atualizar documentacao**: atualize OpenAPI, `docs/` e o roadmap.
7. **Entregar para revisao (@corely-review)**: invoque o subagente `corely-review` com escopo, arquivos alterados, `git diff` e testes. Aguarde o veredito.
8. **Aplicar o veredito**: `CHANGES_REQUESTED` → corrija o plano de correcao e re-entregue; `APPROVED` → marque as stories como `IN_REVIEW` (nunca `DONE`; aprovacao humana torna `DONE`).
9. **Atualizar PROJECT_STATUS** conforme o veredito e **gerar relatorio final**: resumo do que foi feito, o que nao foi e proximos passos.

## Regras

- Siga DDD, Clean Architecture e os padroes do projeto (ver `AI_CONTEXT.md`).
- **Nunca emita `APPROVED`/`CHANGES_REQUESTED`**: veredito exclusivo do `corely-review`.
- **Nunca marque Story como `DONE`**: `IN_REVIEW` apos aprovacao do reviewer; `DONE` somente apos aprovacao humana.
- Nunca quebre contratos REST existentes.
- Nunca crie divida tecnica, TODO, codigo morto ou hardcode.
- Respeite multi-tenant (`studioId`).
- Use `@EntityGraph` e batch queries para evitar N+1.
