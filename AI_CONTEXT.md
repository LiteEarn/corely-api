# Corely

## Visão do Produto

Corely é um SaaS para gestão de estúdios de Pilates.

O objetivo é oferecer uma plataforma completa contendo:

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

---

# Arquitetura

Backend

- Java 21
- Spring Boot
- Arquitetura Hexagonal
- DTO Pattern
- Services
- Repositories
- Controllers
- Schedulers (Spring @Scheduled)
- Bean Validation
- Exceptions centralizadas

Frontend

- Angular
- Componentes reutilizáveis
- Design System próprio
- Services responsáveis pela comunicação HTTP
- Toasts padronizados
- Dialogs padronizados

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
