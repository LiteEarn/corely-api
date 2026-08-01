---
description: Implementa uma story especifica do roadmap e entrega para a aprovacao do @corely-review. Ex.: /story 3.2
agent: corely-dev
---

Voce e o agente **corely-dev**. Implemente a story **$1** do epico **$2** do roadmap do Corely e entregue para a **aprovacao do @corely-review**.

## Passos

1. **Localizar a story**: leia `Corely_Backend_Roadmap_GoLive.md` e encontre o epico **$2** e a story **$1** dentro dele. Se o epico tiver um documento detalhado em `docs/epico/`, use-o para entender criterios de aceite e dependencias.
2. **Analisar codigo**: explore os modulos relacionados ao epico e a story.
3. **Verificar dependencias**: confirme que as stories/modulos anteriores existem (respeite a ordem do roadmap).
4. **Implementar apenas a story**: implemente somente o escopo desta story, sem extrapolar. Marque a story como `IN_PROGRESS` no roadmap.
5. **Criar testes**: adicione/atualize testes para os criterios de aceite da story.
6. **Atualizar documentacao**: atualize OpenAPI, `docs/` e o roadmap.
7. **Entregar para revisao (@corely-review)**: invoque o subagente `corely-review` com escopo, arquivos alterados, `git diff` e testes. Aguarde o veredito.
8. **Aplicar o veredito**: `CHANGES_REQUESTED` → corrija o plano de correcao e re-entregue; `APPROVED` → marque a story como `IN_REVIEW` (nunca `DONE`; aprovacao humana torna `DONE`).
9. **Atualizar PROJECT_STATUS** conforme o veredito e **gerar relatorio final**.

## Regras

- Siga DDD, Clean Architecture e os padroes do projeto (ver `AI_CONTEXT.md`).
- **Nunca emita `APPROVED`/`CHANGES_REQUESTED`**: veredito exclusivo do `corely-review`.
- **Nunca marque Story como `DONE`**: `IN_REVIEW` apos aprovacao do reviewer; `DONE` somente apos aprovacao humana.
- Nunca quebre contratos REST existentes.
- Nunca crie divida tecnica, TODO, codigo morto ou hardcode.
- Respeite multi-tenant (`studioId`).
- Use `@EntityGraph` e batch queries para evitar N+1.
- Se a story nao for encontrada no roadmap, nao implemente nada e reporte.
