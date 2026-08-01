# Produto

## Descrição

Corely é um SaaS para gestão de estúdios de Pilates.

## Objetivo

Oferecer uma plataforma completa para o dia a dia do estúdio:

- Alunos
- Instrutores
- Turmas
- Matrículas
- Agenda
- Presença
- Financeiro
- Dashboard Operacional
- Dashboard Financeiro
- WhatsApp
- Relatórios

## Público alvo

Proprietários e gestores de estúdios de Pilates (pequeno e médio porte) que
desejam digitalizar operação, agenda, matrículas e financeiro em um único lugar.

## Visão do produto

Ser a plataforma de gestão de referência para estúdios de Pilates, reduzindo
trabalho manual, aumentando a aderência de alunos e dando visibilidade completa
de operação e finanças.

## Roadmap

O progresso do produto é controlado pelo roadmap de Go Live
(`Corely_Backend_Roadmap_GoLive.md`) e pelo fluxo de desenvolvimento descrito
neste arquivo. Todo trabalho deve estar alinhado ao roadmap.

## Objetivo de Go Live

Levar o Corely a produção de forma segura, com:

- Segurança e multi-tenancy validados (EPIC 01 e 02).
- Funcionalidades essenciais de operação e financeiro entregues.
- Cobertura de testes, documentação (OpenAPI/Swagger) e roadmaps atualizados.
- Zero dívida técnica acumulada.

---

# Arquitetura

Backend

- Java 21
- Spring Boot
- DDD
- Clean Architecture
- Arquitetura Hexagonal
- Flyway
- PostgreSQL
- DTO Pattern
- Mapper
- Services
- Repositories
- Controllers
- Schedulers (Spring @Scheduled)
- Bean Validation
- Exceptions centralizadas
- OpenAPI / Swagger
- JUnit
- Mockito
- Testcontainers

Frontend

- Angular
- Signals
- Componentes reutilizáveis
- Design System próprio
- Services responsáveis pela comunicação HTTP
- Toasts padronizados
- Dialogs padronizados

---

# Regras obrigatórias

## Nunca

- Criar dívida técnica.
- Quebrar contratos REST existentes.
- Diminuir cobertura de testes.
- Gerar N+1.
- Deixar TODO.
- Criar código morto.
- Duplicar lógica.
- Acessar Repository diretamente pelo Controller.
- Colocar regra de negócio no Controller.
- Permitir acesso entre studios (multi-tenant).
- Alterar arquivos fora da lista permitida.
- Implementar funcionalidades fora do roadmap.

## Sempre

- Analisar código existente antes de implementar.
- Implementar apenas o que falta.
- Criar testes.
- Atualizar documentação.
- Atualizar roadmap.
- Gerar relatório técnico.
- Respeitar `studioId`.
- Reutilizar Services existentes.
- Seguir as convenções de backend e frontend abaixo.

---

# Roadmaps

## Backend

Arquivo: `Corely_Backend_Roadmap_GoLive.md`

Roadmap **executável**: cada épico e story possuem metadados estruturados em
blocos `yaml` (id, status, priority, progress, dependsOn, acceptanceCriteria,
definitionOfDone, stories). É a fonte oficial do planejamento do backend. Os
comandos consomem esses metadados de forma determinística — sem heurísticas de
texto livre.

Status permitidos: `TODO` | `IN_PROGRESS` | `IN_REVIEW` | `BLOCKED` | `DONE`.
Priority: `CRITICAL` | `HIGH` | `MEDIUM` | `LOW`.

`IN_REVIEW` = aprovado pelo `corely-review`, aguardando aprovacao humana para
virar `DONE`. Nenhuma story vira `DONE` sem aprovacao humana.

## Frontend

Arquivo: `Corely_Frontend_Roadmap_GoLive.md` (a criar quando o repositório
frontend for versionado neste projeto)

Estrutura espelhada do backend: épicos, stories e status. Enquanto o arquivo
não existir, o agente `corely-frontend` deve reportar o roadmap frontend como
`NOT_INITIALIZED` e sugerir sua criação.

## Status oficial do projeto

Arquivo: `PROJECT_STATUS.md`

**Fonte única oficial de progresso.** Os commands não calculam progresso — eles
leem e atualizam este arquivo. Atualizado a cada conclusão de story/épico.
Seções: Backend, Frontend, Infraestrutura, Go Live.

---

# Fluxo obrigatório

Todo trabalho deve seguir, nesta ordem:

Analisar

↓

Comparar com roadmap executável (`Corely_Backend_Roadmap_GoLive.md`) e status oficial (`PROJECT_STATUS.md`)

↓

Localizar a Story Atual (ID determinístico nos metadados)

↓

Verificar dependências (`dependsOn`)

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

## Governança de qualidade

- `corely-dev` **implementa, testa e entrega**; nunca decide que a entrega está pronta.
- `corely-review` é a **autoridade máxima de qualidade**: audita (checklist de 11
  seções), emite `APPROVED` ou `CHANGES_REQUESTED` e prepara o PR. Nunca implementa.
- Nenhum código chega a PR sem `APPROVED` do `corely-review`.
- Nenhuma Story vira `DONE` sem aprovação humana (após `IN_REVIEW`).

---

# Convenções

Nunca criar endpoints duplicados.

Nunca alterar contratos REST existentes.

Sempre reutilizar Services.

Nunca duplicar regras de negócio.

Toda regra de negócio pertence ao Backend.

Frontend apenas apresenta informações.

---

# Multi-tenant

Todo dado pertence a um Studio.

Sempre respeitar studioId.

Nunca permitir acesso entre studios.

---

# Convenções Backend

Sempre utilizar:

DTO

Mapper

Service

Repository

Controller

Nunca acessar Repository diretamente pelo Controller.

Nunca colocar regra de negócio no Controller.

---

# Convenções Frontend

Component

Service

Model

Interface

Não duplicar chamadas HTTP.

Evitar subscribe aninhado.

Utilizar RxJS corretamente.

Utilizar mensagens vindas da API.

Todos os textos devem permanecer em português.

---

# Fluxo de Trabalho

O agente nunca decide arquitetura.

O agente nunca cria funcionalidades fora da Task recebida.

O agente nunca realiza melhorias não solicitadas.

O agente nunca altera arquivos fora da lista permitida.

Ao concluir a Task deve parar imediatamente.

---

# Definição de Task

Toda Task possuirá:

Objetivo

Contexto

Arquivos permitidos

Arquivos proibidos

Implementação

Critérios de aceite

Não fazer

Entrega

---

# Regras Gerais

Nunca executar testes automaticamente, exceto quando a Task solicitar explicitamente.

Nunca criar novos componentes sem necessidade.

Nunca alterar Design System sem solicitação.

Nunca alterar Layout.

Nunca alterar autenticação.

Nunca alterar rotas fora da Task.

Nunca realizar refatorações globais.

Sempre implementar apenas o escopo solicitado.

---

# Resposta esperada

Ao concluir a Task retornar somente:

- Arquivo criado
- Localização
- Resumo do conteúdo

Não realizar nenhuma outra alteração no projeto.
