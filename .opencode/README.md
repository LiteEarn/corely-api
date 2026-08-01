# OpenCode — Infraestrutura de IA do Corely

> **AI Platform: v1.0.0**
>
> Esta estrutura é o mecanismo oficial de orquestração do desenvolvimento do
> Corely, usando apenas os recursos oficiais do OpenCode: **Agents**,
> **Commands** e **Skills**.
>
> A partir do EPIC 00 (AI Governance), a plataforma está **congelada**: novas
> funcionalidades de infraestrutura apenas com necessidade comprovada. O foco
> passa a ser exclusivamente o desenvolvimento do produto, guiado pelo roadmap
> executável e pelo `PROJECT_STATUS.md`.

> **Importante**: após alterar qualquer arquivo de config (`opencode.json`,
> agentes, commands, skills), **saia e reabra o opencode** para que as mudanças
> sejam carregadas.

---

## AI Governance

### Fontes oficiais

| Artefato | Papel |
| :--- | :--- |
| `AI_CONTEXT.md` | **Fonte oficial do conhecimento**: produto, arquitetura, regras obrigatórias (nunca/sempre), roadmaps, fluxo obrigatório e convenções. Carregado via `instructions` do `opencode.json`. |
| `Corely_Backend_Roadmap_GoLive.md` | **Fonte oficial do roadmap**: planejamento executável com metadados estruturados em blocos `yaml` por épico e story (id, status, priority, progress, dependsOn, acceptanceCriteria, definitionOfDone, stories). |
| `PROJECT_STATUS.md` | **Fonte única oficial de status/progresso**: Backend, Frontend, Infraestrutura e Go Live. Commands não calculam progresso — apenas leem e atualizam este arquivo. |

### Responsabilidades

- **Agents**: papéis especializados. `corely-dev` executa o fluxo de
  desenvolvimento (implementa, testa e entrega); `corely-review` é a
  **autoridade máxima de qualidade** (audita com checklist de 11 seções, emite
  `APPROVED`/`CHANGES_REQUESTED` e prepara o PR; nunca implementa).
  `corely-architect`, `corely-frontend` e `corely-release` são subagentes sob
  demanda. Nenhum agente define progresso.
- **Skills**: conhecimento reutilizável (técnico, domínio, roadmap, arquitetura
  e qualidade). Nunca implementação.
- **Commands**: orquestração. `/status` renderiza `PROJECT_STATUS.md`;
  `/continue` executa a Story Atual de forma determinística e entrega para
  revisão; `/golive` repete `/continue` até encontrar bloqueador ou story
  `IN_REVIEW`; `/review` e `/quality` são executados pelo `corely-review`.
  Atualizam roadmap + PROJECT_STATUS.

### Regra de ouro da governança

Todo trabalho parte do **roadmap executável** e atualiza o **PROJECT_STATUS**.
Nenhuma feature é adicionada fora do roadmap. Após cada story: implementar,
entregar para `@corely-review`, aplicar o veredito (`APPROVED` → `IN_REVIEW`;
`CHANGES_REQUESTED` → corrigir e re-entregar), aguardar aprovação humana
(`IN_REVIEW` → `DONE`), atualizar roadmap/PROJECT_STATUS e gerar relatório.

### Governança de qualidade

- `corely-dev` implementa, testa e entrega; **nunca** decide que a entrega está pronta.
- `corely-review` é a autoridade máxima: emite `APPROVED` ou `CHANGES_REQUESTED`;
  nenhum código chega a PR sem `APPROVED`.
- Nenhuma Story vira `DONE` sem aprovação humana (após `IN_REVIEW`).

---

## 1. Estrutura de arquivos

```
opencode.json                          # Configuracao do projeto
AI_CONTEXT.md                          # Contexto global (fonte oficial do conhecimento)
Corely_Backend_Roadmap_GoLive.md       # Roadmap executavel (metadados yaml por epic/story)
PROJECT_STATUS.md                      # Status oficial do projeto (fonte unica de progresso)
.opencode/
  agents/                              # Agentes (assistentes especializados)
    corely-dev.md                      #   Agente principal (primary)
    corely-review.md                   #   Autoridade maxima de qualidade (subagent)
    corely-architect.md                #   Arquitetura e modelagem (subagent)
    corely-frontend.md                 #   Frontend Angular (subagent)
    corely-release.md                  #   Release/Deploy/Go Live (subagent)
  commands/                            # Comandos reutilizaveis
    epic.md                            #   /epic <numero>
    story.md                           #   /story <epic.story>
    review.md                          #   /review <pr>
    continue.md                        #   /continue
    status.md                          #   /status
    quality.md                         #   /quality
    golive.md                          #   /golive
  skills/                              # Skills (conhecimento reutilizavel)
    spring-boot/SKILL.md
    testing/SKILL.md
    security/SKILL.md
    multi-tenant/SKILL.md
    persistence/SKILL.md
    rest-api/SKILL.md
    ddd/SKILL.md
    performance/SKILL.md
    release/SKILL.md
    corely-domain/SKILL.md
    corely-roadmap/SKILL.md
    corely-architecture/SKILL.md
    corely-quality/SKILL.md
```

---

## 2. Papel de cada arquivo

### `opencode.json`

Configuracao do projeto:

- `default_agent: "corely-dev"` — o `corely-dev` e o agente padrao de todas as sessoes.
- `instructions: ["AI_CONTEXT.md"]` — o contexto global (produto, arquitetura,
  regras obrigatorias, roadmaps, fluxo) e sempre carregado.
- `skills.paths` — registra a pasta `.opencode/skills`.

### `AI_CONTEXT.md`

Fonte oficial do conhecimento. Contém:

- **Produto**: descrição, objetivo, público alvo, visão, roadmap e objetivo de Go Live.
- **Arquitetura**: stack (Java 21, Spring Boot, DDD, Clean Architecture, Flyway,
  PostgreSQL, Angular, Signals, Design System, REST, OpenAPI, JUnit, Mockito, Testcontainers).
- **Regras obrigatórias**: nunca/sempre (dívida técnica, contratos, cobertura, N+1, TODOs, etc.).
- **Roadmaps**: backend (executável) e frontend, mais o status oficial (`PROJECT_STATUS.md`).
- **Fluxo obrigatório**: analisar → comparar com roadmap/status → localizar Story
  Atual → verificar dependências → implementar → testar → atualizar roadmap →
  atualizar PROJECT_STATUS → relatório.
- **Convenções**: multi-tenant, backend, frontend, fluxo de trabalho e resposta esperada.

### `Corely_Backend_Roadmap_GoLive.md`

Roadmap executável. Cada épico/story tem metadados estruturados em `yaml`.
Status: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `BLOCKED` | `DONE`. Priority:
`CRITICAL` | `HIGH` | `MEDIUM` | `LOW`.
`IN_REVIEW` = aprovado pelo `corely-review`, aguardando aprovação humana.

### `PROJECT_STATUS.md`

Status oficial do projeto: Backend, Frontend, Infraestrutura e Go Live.
Única fonte oficial de progresso.

### Agentes (`.opencode/agents/`)

| Arquivo | Modo | Papel |
| :--- | :--- | :--- |
| `corely-dev.md` | `primary` | Agente principal. Conhece arquitetura, segue DDD/Clean Architecture, nunca cria dívida técnica, sempre atualiza testes/docs/roadmap/PROJECT_STATUS. Implementa, testa e entrega para `@corely-review`; nunca emite `APPROVED` nem marca `DONE`. Executa os comandos de fluxo (/continue, /golive, /status, etc.). |
| `corely-review.md` | `subagent` | Autoridade máxima de qualidade. Audita (checklist de 11 seções), emite `APPROVED`/`CHANGES_REQUESTED` e prepara o PR. Nunca implementa. Só leitura (`edit: deny`). |
| `corely-architect.md` | `subagent` | Decisões de arquitetura, modelagem de domínio, APIs, banco, eventos, integrações. Só leitura. |
| `corely-frontend.md` | `subagent` | Angular, Signals, Design System, UX, responsividade. |
| `corely-release.md` | `subagent` | Go Live, Deploy, Release, CI/CD, produção. |

### Commands (`.opencode/commands/`)

| Comando | Exemplo | O que faz |
| :--- | :--- | :--- |
| `/status` | `/status` | Renderiza o estado oficial a partir de `PROJECT_STATUS.md`. Não calcula progresso. |
| `/continue` | `/continue` | Executa a Story Atual de forma determinística (PROJECT_STATUS → roadmap → dependências → implementar → testar → entregar para `@corely-review` → aplicar veredito: `APPROVED` → `IN_REVIEW` / `CHANGES_REQUESTED` → corrigir e re-entregar → atualizar roadmap e PROJECT_STATUS → relatório). |
| `/golive` | `/golive` | Repete o fluxo de `/continue` enquanto existir story desbloqueada e aprovada; para no bloqueador ou em story `IN_REVIEW` (aguardando aprovação humana) e gera relatório. |
| `/epic` | `/epic 03` | Implementa um épico do roadmap (metadados estruturados), testa, entrega para revisão e atualiza roadmap/PROJECT_STATUS conforme o veredito. |
| `/story` | `/story 3.2` | Implementa apenas a story `$1` do épico `$2` e entrega para revisão. |
| `/review` | `/review 125` | Revisão completa do PR pelo `corely-review` (autoridade máxima), com veredito `APPROVED`/`CHANGES_REQUESTED`. |
| `/quality` | `/quality` | Auditoria completa da implementação pelo `corely-review` (11 seções), com veredito e notas por seção. |

### Skills (`.opencode/skills/<nome>/SKILL.md`)

Cada skill contém apenas **conhecimento reutilizavel** (nunca implementacao):

- **Técnicas**: `spring-boot`, `testing`, `security`, `multi-tenant`,
  `persistence`, `rest-api`, `ddd`, `performance`, `release`.
- **Domínio**: `corely-domain` — conhecimento do domínio de negócio (aluno,
  matrícula, plano, cobrança, agenda, presença, evolução, avaliação, booking,
  financeiro). Nunca contém implementação.
- **Roadmap**: `corely-roadmap` — interpretação do roadmap executável (metadados
  `yaml`), dependências, progresso e manutenção do `PROJECT_STATUS.md`.
- **Arquitetura**: `corely-architecture` — módulos, packages, convenções e o
  padrão DTO/Mapper/Service/Repository/Controller.
- **Qualidade**: `corely-quality` — checklist obrigatório de qualquer
  implementação (11 seções: arquitetura, domínio, multi-tenant, segurança,
  banco, performance, API, código, testes, documentação, qualidade). Fonte única
  de qualidade (substituiu `pr-review`).

---

## 3. Como usar no dia a dia

- **Ver o status oficial**: digite `/status`.
- **Continuar de onde parou**: digite `/continue`.
- **Fluxo automático até Go Live**: digite `/golive`.
- **Implementar um épico**: digite `/epic 03`.
- **Implementar uma story**: digite `/story 3.2`.
- **Revisar um PR**: digite `/review 125`.
- **Auditar a qualidade**: digite `/quality`.

Voce tambem pode invocar subagentes manualmente com `@`:

- `@corely-review` — auditar uma entrega e obter veredito `APPROVED`/`CHANGES_REQUESTED`.
- `@corely-architect` — discutir modelagem antes de implementar.
- `@corely-frontend` — tarefas de Angular.
- `@corely-release` — preparar deploy.

---

## 4. Como funciona o fluxo de desenvolvimento

Todo trabalho segue o **fluxo obrigatório** definido em `AI_CONTEXT.md`:

```
Analisar
   ↓
Comparar com roadmap executável e PROJECT_STATUS
   ↓
Localizar a Story Atual (ID nos metadados)
   ↓
Verificar dependências (dependsOn)
   ↓
Implementar somente o necessário
   ↓
Criar testes
   ↓
Executar testes
   ↓
Entregar para revisão (@corely-review)
   ↓
Aplicar veredito (CHANGES_REQUESTED → corrigir e re-entregar; APPROVED → Story IN_REVIEW)
   ↓
Aprovação humana (Story IN_REVIEW → DONE)
   ↓
Atualizar roadmap (status/metadados)
   ↓
Atualizar PROJECT_STATUS (fonte única de progresso)
   ↓
Gerar relatório
```

Os comandos de fluxo (`/continue`, `/golive`, `/epic`, `/story`) automatizam
esse ciclo de forma determinística: leem `PROJECT_STATUS.md`, localizam a story
pelo ID nos metadados `yaml` do roadmap, verificam dependências, implementam
apenas o que falta, testam, entregam para `@corely-review` e aplicam o veredito
(`APPROVED` → `IN_REVIEW`, aguardando aprovação humana; `CHANGES_REQUESTED` →
corrigir e re-entregar). Quando há bloqueador, o fluxo para e reporta.

### Governança de qualidade

- `corely-dev` implementa, testa e entrega; nunca decide que a entrega está pronta.
- `corely-review` é a autoridade máxima de qualidade: audita (checklist de 11
  seções), emite `APPROVED` ou `CHANGES_REQUESTED` e prepara o PR. Nunca implementa.
- Nenhum código chega a PR sem `APPROVED` do `corely-review`.
- Nenhuma Story vira `DONE` sem aprovação humana (após `IN_REVIEW`).

---

## 5. Como adicionar novos agentes

Crie um arquivo em `.opencode/agents/<nome>.md`:

```markdown
---
description: O que o agente faz e quando usar.
mode: subagent
temperature: 0.1
permission:
  edit: deny
---

Voce e o agente <nome>. Faca X, Y e Z.
```

- `mode`: `primary` (tab para alternar) ou `subagent` (invocado via `@`/Task).
- `permission`: controle de acesso (`edit`, `bash`, `webfetch`, etc.).
- O nome do arquivo vira o nome do agente.

Depois, **reabra o opencode**.

---

## 6. Como adicionar novos commands

Crie um arquivo em `.opencode/commands/<nome>.md`:

```markdown
---
description: Descricao curta do comando.
agent: corely-dev
---

Prompt que sera executado. Use $ARGUMENTS para o que o usuario digitar
e $1/$2 para argumentos posicionais.
```

O nome do arquivo vira o comando (`/nome`). Depois, **reabra o opencode**.

> Plataforma congelada: novos commands apenas com necessidade comprovada.

---

## 7. Como adicionar novas Skills

Crie uma pasta em `.opencode/skills/<nome>/` com um arquivo `SKILL.md`:

```markdown
---
name: <nome>
description: O que a skill faz E quando disparar. Use "Use quando...".
---

# <Nome>

Conhecimento reutilizavel, sem implementacao.
```

Regras:

- `name` em minusculas com hifens (ex.: `multi-tenant`), 1-64 chars, e deve
  ser igual ao nome da pasta.
- `description` obrigatoria (1-1024 chars).
- Cada skill deve conter apenas conhecimento; nunca codigo de implementacao.
- Skills de domínio (`corely-domain`) devem conter apenas conhecimento de
  negócio, nunca detalhes de implementação.

Depois, **reabra o opencode**.

---

## 8. Como evoluir a estrutura

- **Novo dominio no backend**: use `corely-domain` para documentar o conceito e
  `corely-architecture` para o padrão de módulos; `ddd`/`spring-boot` cobrem o processo.
- **Novo fluxo de desenvolvimento**: crie um command que siga o fluxo
  obrigatório de `AI_CONTEXT.md` (analisar → implementar → testar → entregar
  para `@corely-review` → aplicar veredito → atualizar roadmap e
  PROJECT_STATUS → relatório).
- **Nova regra de qualidade**: amplie `corely-quality` (fonte única de qualidade).
- **Novo fluxo de release**: amplie `release`.
- **Nova convencao global**: adicione em `AI_CONTEXT.md` (ja carregado via `instructions`).

Sempre siga a documentacao oficial do OpenCode:

- Commands: https://opencode.ai/docs/commands/
- Agents: https://opencode.ai/docs/agents/
- Skills: https://opencode.ai/docs/skills/
- Config: https://opencode.ai/docs/config/

**Nao invente formatos**: use apenas os campos documentados. Configuracao
invalida impede o opencode de iniciar.
