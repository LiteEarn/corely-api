---
name: corely-roadmap
description: Ensinar a interpretar o roadmap executavel de Go Live do Corely (metadados estruturados em yaml), escolher a proxima story, identificar dependencias, atualizar progresso e marcar itens concluidos. Tambem ensina a manter PROJECT_STATUS.md como fonte oficial do estado. Use ao trabalhar com roadmap/status ou nos comandos /status, /continue, /golive.
---

# Como trabalhar com o Roadmap Corely

Guia de uso do roadmap executável do Corely (`Corely_Backend_Roadmap_GoLive.md`)
e do status oficial (`PROJECT_STATUS.md`).

## Fontes oficiais

- **Roadmap**: `Corely_Backend_Roadmap_GoLive.md` — fonte oficial do
  planejamento (épicos e stories).
- **Status**: `PROJECT_STATUS.md` — fonte oficial do estado/progresso do projeto.
- **Contexto**: `AI_CONTEXT.md` — produto, arquitetura, regras e fluxo.

## Estrutura do roadmap

- O roadmap é organizado por **EPICs** (ID `EPIC-01`..`EPIC-09`) e **stories**
  (ID `EPIC-XX-SNN`).
- Cada épico e cada story possuem um bloco de **metadados estruturados em
  `yaml`** embutido no Markdown. Esses blocos são a representação
  determinística para agentes — **sem heurísticas de texto livre**.
- Campos obrigatórios por épico: `id`, `title`, `description`, `status`,
  `priority`, `progress`, `dependsOn`, `acceptanceCriteria`,
  `definitionOfDone`, `stories`.
- Campos obrigatórios por story: `id`, `title`, `description`, `status`,
  `priority`, `estimate`, `dependsOn`, `acceptanceCriteria`,
  `definitionOfDone`, `files`.

## Status permitidos

`TODO` | `IN_PROGRESS` | `IN_REVIEW` | `BLOCKED` | `DONE`

`IN_REVIEW` = implementacao aprovada pelo `corely-review` (veredito
`APPROVED`), aguardando aprovacao humana para virar `DONE`. Nenhuma story vira
`DONE` sem aprovacao humana.

## Como localizar a próxima story

1. Ler `PROJECT_STATUS.md` → campo **Story Atual** (ex.: `EPIC-01-S01`).
2. Localizar a story pelo **ID** nos metadados `yaml` do roadmap.
3. Se a Story Atual estiver `DONE`, a próxima é determinada pela ordem das
   stories no épico e pelos `dependsOn`; atualizar `PROJECT_STATUS.md` em
   seguida.
4. Se a Story Atual estiver `IN_REVIEW`, aguardar a aprovacao humana antes de
   avancar para a proxima (nunca pular uma story `IN_REVIEW`).

## Como identificar dependências

- Verificar o campo `dependsOn` da story e do épico (IDs de stories/épicos).
- Uma story está **bloqueada** se qualquer dependência não está `DONE`.
- Se bloqueada: marcar como `BLOCKED`, registrar o motivo em
  `PROJECT_STATUS.md` e reportar.

## Como atualizar progresso

- Apos concluir a implementacao e os testes, entregar para o `corely-review`
  (veredito `APPROVED`/`CHANGES_REQUESTED`).
- Se `APPROVED`: faca **commit, push e PR**:
  1. `git add` dos arquivos alterados.
  2. `git commit` com mensagem no formato `tipo(escopo): descricao (STORY_ID)`.
  3. `git push` para a branch atual.
  4. `gh pr create` com titulo e corpo descritivos.
  5. Marcar `status: IN_REVIEW` no roadmap.
  6. Atualizar `PROJECT_STATUS.md` (Story Atual permanece nela, aguardando
     aprovacao humana).
- Se `CHANGES_REQUESTED`: manter `IN_PROGRESS`, corrigir o plano de correcao e
  re-entregar para nova revisao.
- Apos aprovacao humana: marcar `status: DONE` e atualizar `PROJECT_STATUS.md`
  (proxima Story Atual, percentual, bloqueadores).
- `progress` do epico e percentual (0-100) calculado por stories concluidas.
- Nunca antecipar conclusao: marcar `DONE` apenas com codigo, testes, docs,
  commit/push/PR e aprovacao humana.

## Como marcar itens concluídos

- Story: `IN_REVIEW` quando aprovada pelo `corely-review` e PR aberto; `DONE`
  apenas apos aprovacao humana.
- Épico: `DONE` quando todas as stories estiverem `DONE`.

## Regras

- Não inventar IDs, status ou campos: usar exatamente os metadados existentes.
- `PROJECT_STATUS.md` é a única fonte oficial de progresso; commands não
  calculam progresso — apenas leem/atualizam o arquivo.
