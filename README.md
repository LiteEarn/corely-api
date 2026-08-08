# corely-api

API do Corely — SaaS de gestão de studios de Pilates (Java 21 / Spring Boot).

## Pré-requisitos

- JDK 21
- Maven (ou use o wrapper `./mvnw`)
- PostgreSQL 17 (ou `docker compose up -d postgres`)

## Configuração de ambiente

A API resolve segredos e configurações por **variáveis de ambiente**. Nenhum segredo é versionado.

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | **Sim** (fora do profile `dev`) | Chave de assinatura HMAC-SHA dos tokens JWT (min. 32 bytes). Em produção deve ser definida explicitamente — a aplicação falha ao iniciar sem ela. |
| `JWT_PREVIOUS_SECRETS` | Não | Lista separada por vírgula de segredos JWT anteriores (rotação). Tokens assinados com esses segredos continuam válidos até expirarem. |
| `DATABASE_URL` | **Sim** (profile `prod`) | URL JDBC do PostgreSQL com schema (ex.: `jdbc:postgresql://host:5432/corely?currentSchema=corely`). O profile `prod` falha ao iniciar sem ela. |
| `DATABASE_USERNAME` | **Sim** (profile `prod`) | Usuário do banco de produção. |
| `DATABASE_PASSWORD` | **Sim** (profile `prod`) | Senha do banco de produção. |
| `CORS_ALLOWED_ORIGINS` | **Sim** (profile `prod`) | Origens permitidas no CORS (separadas por vírgula). O profile `prod` falha ao iniciar sem ela. |

### JWT

- **Produção / demais profiles**: o segredo é resolvido de `JWT_SECRET` (`application.yaml`). Sem a variável definida, a aplicação não inicia (fail-fast).
- **Dev local** (`--spring.profiles.active=dev`): usa `JWT_SECRET` se definida; caso contrário, um valor default exclusivo de desenvolvimento em `application-dev.yaml` (`dev-only-secret-not-for-production-corely`).
- **Testes** (`@ActiveProfiles("test")`): segredo exclusivo de teste em `application-test.properties`.

Exemplo de execução local com perfil dev:

```bash
JWT_SECRET=$(openssl rand -base64 48) ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Rotação de segredo JWT (sem downtime)

Para rotacionar o segredo sem derrubar a aplicação nem invalidar tokens ativos:

1. **Defina o novo segredo como `JWT_SECRET`** e mova o segredo atual para `JWT_PREVIOUS_SECRETS` (lista separada por vírgula):

   ```bash
   JWT_SECRET=$(openssl rand -base64 48)
   JWT_PREVIOUS_SECRETS=<segredo-antigo>
   ```

2. **Deploy da nova configuração.** Tokens emitidos com o segredo anterior continuam sendo aceitos até sua expiração natural.
3. **Após o período de expiração dos tokens antigos** (ver `jwt.refresh-token-expiration`), remova o segredo anterior de `JWT_PREVIOUS_SECRETS` e faça novo deploy.

O `JwtService` assina novos tokens apenas com `JWT_SECRET` (atual) e valida tokens usando o segredo atual **e** os anteriores — garantindo transição suave.

## Perfis de execução

| Profile | Uso | Características |
|---|---|---|
| (nenhum) | Desenvolvimento base | `application.yaml` — datasource local com defaults (`localhost:5432`), sem SQL logging. |
| `dev` | Desenvolvimento local | `application-dev.yaml` — seed habilitado, porta `8081`. |
| `test` | Testes automatizados | `application-test.properties` — H2 em memória. |
| `prod` | Produção | `application-prod.yaml` — **isolado e seguro** (detalhes abaixo). |

### Profile `prod` (produção)

Configuração dedicada e segura para produção em `application-prod.yaml`:

- **Datasource via variáveis de ambiente** (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`) — **sem credenciais embutidas** e **fail-fast** se ausentes (a aplicação não inicia com banco local por engano).
- **SQL logging desabilitado**: `show-sql: false` e `format_sql: false`; nível `org.hibernate.SQL: WARN`.
- **Schema validado, nunca alterado**: `ddl-auto: validate` (migrations via Flyway).
- **Seed desabilitado em produção**: além de `corely.seed.enabled: false`, o seed automático e o endpoint `/dev/seed/**` são **exclusivos do perfil `dev`** — em qualquer outro ambiente o seed nunca é criado nem exposto, mesmo que a flag seja definida como `true`.
- **Swagger/OpenAPI desabilitado**: `springdoc.api-docs.enabled: false` e `springdoc.swagger-ui.enabled: false`, e `corely.swagger.enabled: false` (a segurança deixa de permitir acesso público aos paths de documentação — mesmo que a documentação seja re-habilitada, o Swagger exige autenticação).
- **Stacktraces não expostos** em respostas de erro (`server.error.include-stacktrace: never`).
- **JWT fail-fast**: `JWT_SECRET` obrigatório sem default.

Execução em produção:

```bash
export DATABASE_URL="jdbc:postgresql://<host>:5432/corely?currentSchema=corely"
export DATABASE_USERNAME="<user>"
export DATABASE_PASSWORD="<password>"
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Swagger / OpenAPI

A documentação da API fica disponível em `/swagger-ui` e `/v3/api-docs` por padrão (perfil `dev` e demais, via `corely.swagger.enabled: true`).

No profile `prod` o Swagger é **protegido**:
- A geração da documentação e da UI é **desabilitada** (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`) — os endpoints não existem.
- A camada de segurança deixa de liberar os paths (`corely.swagger.enabled: false`) — mesmo que a documentação seja re-habilitada, os endpoints exigem autenticação (requisições sem token são rejeitadas).

## Seed de dados

O seed de dados é **exclusivo do desenvolvimento**:
- **Seed automático** (`SeedRunner`): executado no boot apenas quando o perfil ativo é `dev` **e** `corely.seed.enabled=true` — condição fail-closed no `SeedConfiguration` (o perfil é o gate obrigatório).
- **Endpoint `/dev/seed/**`** (`SeedController`): registrado apenas no perfil `dev` (`@Profile("dev")`) — fora do dev o endpoint não existe.

Em produção, portanto, o seed **nunca** é executado nem exposto, independentemente de `corely.seed.enabled` — garantido estruturalmente.

## Rate limiting

A API aplica **rate limiting por endereço IP** para proteger endpoints sensíveis contra brute force e abuso (EPIC-02-S06):

- **Limite global**: todas as rotas (exceto as sensíveis) são limitadas por `corely.rate-limit.requests-per-window` requisições por janela de `corely.rate-limit.window-seconds` segundos (padrão: 100 requisições / 60s).
- **Endpoints sensíveis**: rotas listadas em `corely.rate-limit.sensitive-paths` (padrão: `/auth/**`) recebem um limite mais restrito via `corely.rate-limit.sensitive-requests-per-window` (padrão: 5 requisições / 60s).
- **Escopos independentes**: os limites global e sensível possuem buckets separados por IP — tráfego global não "reabastece" o limite de login, e esgotar as tentativas de login não bloqueia os demais endpoints.
- **Resposta**: quando o limite é excedido, a API responde `429 Too Many Requests` com o header `Retry-After`.
- **Preflight OPTIONS (CORS)**: requisições `OPTIONS` não consomem tokens (consistentes com o `permitAll` existente).
- **Identificação do cliente**: por padrão usa o `remoteAddr`. Se houver um proxy de ingresso **confiável que sobrescreva** o header `X-Forwarded-For` (nunca apenas anexe), defina `corely.rate-limit.trust-forwarded-header: true` para usar o primeiro valor do header. Não habilite essa flag sem o proxy sanitizando o header — caso contrário o cliente pode forjar o IP e contornar o limite.
- **Desabilitação**: `corely.rate-limit.enabled: false` desativa o filtro (usado no profile de teste, onde os testes de integração executam muitas requisições do mesmo IP).

No profile `prod` o rate limiting está **habilitado** com os limites padrão. O rate limiter é in-memory (token bucket), o que é adequado para uma instância única; para múltiplas instâncias em cluster, uma solução distribuída (ex.: Redis) é recomendada.

## Lockout de login

A API **bloqueia temporariamente** o login de um e-mail após tentativas inválidas consecutivas, mitigando brute force sobre a autenticação (EPIC-02-S07):

- **Limite**: `corely.login-lockout.max-attempts` tentativas inválidas (padrão: 5) em uma janela de `corely.login-lockout.lockout-seconds` segundos (padrão: 900 = 15 min).
- **Resposta**: enquanto bloqueado, `POST /auth/login` responde `429 Too Many Requests` com código de erro `LOGIN_LOCKED` e o header `Retry-After` com o tempo restante.
- **Reset**: um login bem-sucedido limpa as tentativas do e-mail; após a janela expirar, a contagem recomeça automaticamente.
- **Identificação**: o rastreamento é por **e-mail** (normalizado em minúsculas), independente do IP — complementa o rate limiting por IP da S06.
- **Desabilitação**: `corely.login-lockout.enabled: false` desativa o lockout.

No profile `prod` o lockout está **habilitado** com os limites padrão. Assim como o rate limiter, o rastreamento é in-memory (adequado para instância única; para cluster, uma solução distribuída é recomendada).

## CORS por ambiente

As **origens permitidas** no CORS são configuráveis por ambiente (EPIC-02-S08):

- **Dev / demais profiles**: `corely.cors.allowed-origins` em `application.yaml` (padrão: `http://localhost:4200`).
- **Produção**: `corely.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}` — **fail-fast** sem a variável, e **sem origens hardcoded**. Ex.: `CORS_ALLOWED_ORIGINS=https://app.corely.com.br,https://admin.corely.com.br`.

Os métodos, headers e credenciais permanecem fixos: `GET/POST/PUT/DELETE/OPTIONS/PATCH`, headers liberados (`*`), header `Authorization` exposto e credenciais habilitadas.

## Auditoria LGPD

A API mantém uma **trilha de auditoria** com eventos relevantes para rastreabilidade e conformidade LGPD (EPIC-02-S09):

- **Eventos auditados**: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`, `TOKEN_REFRESH` e `LOCKOUT_TRIGGERED` (autenticação e segurança).
- **Informações registradas** (tabela `corely.audit_logs`): quem (usuário), onde (estúdio), quando, de onde (IP), o que foi feito (evento + recurso + detalhes).
- **Imutabilidade**: um log é criado uma única vez e nunca alterado — trilha confiável para auditoria.
- **Multi-tenant**: a consulta é sempre restrita ao estúdio corrente; nenhum estúdio enxerga logs de outro.
- **Consulta**: `GET /audit-logs` — restrito a `OWNER` e `ADMIN`, com filtros por evento, usuário e intervalo de datas (paginado).

## Contas a Receber — Recebíveis

A API modela **recebíveis** (títulos a receber de alunos) no domínio financeiro (EPIC-03-S01):

- **Criação**: `POST /finance/receivables` — cria um recebível em situação `OPEN` com aluno, valor e vencimento. Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **Consulta**: `GET /finance/receivables` — paginado, com filtros por situação (`OPEN`/`PAID`/`CANCELLED`), aluno e intervalo de vencimentos. `GET /finance/receivables/{id}` para detalhe. Acessível também a `RECEPTIONIST`.
- **Multi-tenant**: recebíveis são sempre restritos ao estúdio corrente (filtro Hibernate + consulta explícita por `studioId`); um estúdio nunca enxerga títulos de outro.
- **Persistência**: tabela `corely.receivables` (migration `V3__receivables.sql`).

## Contas a Receber — Parcelas

As **parcelas** são o desdobramento mensal de cobrança de um aluno matriculado em um plano (EPIC-03-S02):

- **Geração automática**: ao matricular um aluno (`enroll`), o sistema cria o recebível mestre e as **parcelas mensais** — o número de parcelas deriva da duração do plano (30 dias por parcela) e o valor total é o preço mensal × número de parcelas.
- **Consulta**: `GET /finance/installments` — paginado, com filtros por situação, matrícula/plano e intervalo de vencimentos. `GET /finance/installments/{id}` para detalhe. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Multi-tenant**: parcelas sempre restritas ao estúdio corrente.
- **Persistência**: tabela `corely.receivable_installments` (migration `V4__receivable_installments.sql`).

## Contas a Receber — Situação

A **situação financeira** de cada recebível e parcela é calculada a partir do status persistido e do vencimento (EPIC-03-S03):

- **Situações**: `OPEN` (em aberto, não vencido), `PAID` (paga), `OVERDUE` (vencida — em aberto com vencimento no passado) e `REVERSED` (estornada/cancelada).
- **Exposição**: o campo `situation` é incluído nas respostas de `GET /finance/receivables`, `GET /finance/installments` e nos detalhes por ID.
- **Filtro por situação**: `GET /finance/receivables?situation=OVERDUE` e `GET /finance/installments?situation=OVERDUE` — filtram pela situação calculada (em aberto, paga, vencida ou estornada). O filtro por status persistido continua disponível (backward compatible).

## Contas a Receber — Vencimentos

As **datas de vencimento** são controladas e consultáveis (EPIC-03-S04):

- **Consultáveis**: `GET /finance/receivables?dueDateFrom=...&dueDateTo=...` e `GET /finance/installments?dueDateFrom=...&dueDateTo=...` filtram por intervalo de vencimento (paginado).
- **Reagendamento**: `PATCH /finance/receivables/{id}/due-date` e `PATCH /finance/installments/{id}/due-date` atualizam a data de vencimento de um título/parcela em aberto (body: `{"dueDate": "2026-12-10"}`). Não é permitido reagendar títulos **pagos** ou **estornados** (409). Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.

## Contas a Receber — Histórico

O **histórico de movimentações** do recebível registra os eventos do seu ciclo de vida (EPIC-03-S05):

- **Multi-tenant**: histórico sempre restrito ao estúdio corrente.
- **Persistência**: tabela `corely.receivable_movements` (migration `V5__receivable_movements.sql`).

## Pagamentos — Baixa manual

A **baixa manual** registra a liquidação de um recebível (ou de uma parcela específica) e atualiza a situação para paga (EPIC-03-S06):

- **Registro**: `POST /finance/payments` — body com `receivableId`, `paymentDate`, `amount`, `paymentMethod` (CASH, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, OTHER) e opcionais `installmentId`, `externalReference` e `notes`. Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **Regras**: o recebível deve estar em aberto (`OPEN`) e não ter pagamento prévio; o valor deve ser igual ao do recebível (ou da parcela); ao pagar uma parcela, o recebível só é liquidado quando não restam parcelas em aberto.
- **Consulta**: `GET /finance/payments` (paginado, por data decrescente) e `GET /finance/payments/{id}`. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Histórico**: a baixa registra uma movimentação `PAYMENT` no histórico do recebível (`GET /finance/receivables/{id}/movements`).
- **Multi-tenant**: pagamentos sempre restritos ao estúdio corrente.
- **Persistência**: tabela `corely.payments` (migration `V6__payments.sql`).

## Pagamentos — Pix

A **cobrança Pix** gera um pagamento via Pix para um recebível em aberto e permite a **conciliação** (EPIC-03-S07):

- **Geração**: `POST /finance/pix/payments` — body com `receivableId` (obrigatório) e `expiresAt` (opcional; padrão 24h). Retorna `txid` e código **copia-e-cola**. Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **Regras**: o recebível deve estar em aberto (`OPEN`), sem pagamento prévio e sem cobrança Pix pendente. O código copia-e-cola é um placeholder determinístico (integração com PSP fora do escopo), carregando o `txid` para conciliação.
- **Conciliação**: `POST /finance/pix/payments/{txid}/confirm` — confirma o pagamento, registra a baixa (método `PIX`) via `POST /finance/payments`, liquida o recebível e registra a movimentação `PAYMENT` no histórico. Cobranças expiradas são rejeitadas (409) e marcadas como `EXPIRED`.
- **Consulta**: `GET /finance/pix/payments` (paginado) e `GET /finance/pix/payments/{id}`. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Multi-tenant**: cobranças Pix sempre restritas ao estúdio corrente.
- **Persistência**: tabela `corely.pix_payments` (migration `V7__pix_payments.sql`).

## Pagamentos — Cartão

A **cobrança no cartão** gera um pagamento via cartão para um recebível em aberto e permite a **confirmação** (EPIC-03-S08):

- **Geração**: `POST /finance/card/payments` — body com `receivableId` (obrigatório), `cardBrand` (obrigatório, máx. 32), `lastFourDigits` (obrigatório, 4 dígitos), `installments` (opcional, 1–12, padrão 1) e `expiresAt` (opcional; padrão 24h). Retorna `transactionId`. Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **PCI DSS**: apenas bandeira e últimos 4 dígitos do cartão são armazenados — nunca dados completos do cartão.
- **Regras**: o recebível deve estar em aberto (`OPEN`), sem pagamento prévio e sem cobrança de cartão pendente.
- **Confirmação**: `POST /finance/card/payments/{transactionId}/confirm` — confirma o pagamento, registra a baixa (método `CREDIT_CARD`) via `POST /finance/payments`, liquida o recebível e registra a movimentação `PAYMENT` no histórico. Cobranças expiradas são rejeitadas (409) e marcadas como `EXPIRED`.
- **Consulta**: `GET /finance/card/payments` (paginado) e `GET /finance/card/payments/{id}`. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Multi-tenant**: cobranças de cartão sempre restritas ao estúdio corrente.
- **Persistência**: tabela `corely.card_payments` (migration `V8__card_payments.sql`).

## Pagamentos — Dinheiro

O **pagamento em dinheiro** registra a liquidação de um recebível (ou parcela) em dinheiro, de forma imediata (EPIC-03-S09):

- **Registro**: `POST /finance/cash/payments` — body com `receivableId` (obrigatório), `paymentDate` (obrigatório), `amount` (obrigatório, positivo) e opcionais `installmentId` e `notes`. A forma de pagamento é sempre `CASH`. Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **Regras**: reutiliza a baixa manual (`POST /finance/payments` com método `CASH`) — o recebível deve estar em aberto (`OPEN`), sem pagamento prévio; o valor deve ser igual ao do recebível (ou da parcela); registra a movimentação `PAYMENT` no histórico.
- **Consulta**: `GET /finance/cash/payments` (paginado, por data decrescente), filtrando apenas pagamentos em dinheiro. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Multi-tenant**: pagamentos em dinheiro sempre restritos ao estúdio corrente.
- **Persistência**: reutiliza a tabela `corely.payments` (migration `V6__payments.sql`).

## Pagamentos — Estorno

O **estorno de pagamento** reverte uma baixa manual, devolvendo o recebível (ou parcela) à situação de aberto (EPIC-03-S10):

- **Registro**: `POST /finance/refunds` — body com `paymentId` (obrigatório) e `reason` (opcional, motivo do estorno). Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **Regras**: o pagamento deve existir no estúdio corrente e não estar estornado. Ao estornar, o recebível (e a parcela, quando houver) volta para `OPEN` e o pagamento é marcado como estornado (`refundedAt`).
- **Histórico**: o estorno registra uma movimentação `REFUND` no histórico do recebível (`GET /finance/receivables/{id}/movements`).
- **Consulta**: `GET /finance/refunds` (paginado, por data de estorno decrescente), filtrando apenas pagamentos estornados. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Multi-tenant**: estornos sempre restritos ao estúdio corrente.
- **Persistência**: coluna `refunded_at` na tabela `corely.payments` (migration `V9__refunds.sql`).

## Fluxo de Caixa — Entradas

O **fluxo de caixa** registra os movimentos (entradas) de caixa do estúdio (EPIC-03-S11):

- **Registro**: `POST /finance/cash-flow/entries` — body com `entryType` (`ENTRY` ou `OUTFLOW`, obrigatório), `entryDate` (obrigatório), `amount` (obrigatório, positivo), `description` (obrigatório, máx. 500), `source` (`PAYMENT` ou `MANUAL`, obrigatório) e opcionais `paymentId` (obrigatório quando `source=PAYMENT`) e `category` (máx. 50). Restrito a `OWNER`, `ADMIN` e `FINANCIAL`.
- **Regras**: quando a origem é `PAYMENT`, o pagamento deve existir no estúdio corrente (404 caso contrário); quando `MANUAL`, não há pagamento associado. **Saídas** (`OUTFLOW`) nunca são originadas de pagamento — pagamentos geram entradas; uma saída deve ser lançada manualmente (409 caso contrário) (EPIC-03-S12).
- **Consulta**: `GET /finance/cash-flow/entries` (paginado, por data decrescente) com filtros opcionais `entryType`, `dateFrom` e `dateTo`; `GET /finance/cash-flow/entries/{id}`. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST`.
- **Saldo**: `GET /finance/cash-flow/entries/balance` — calcula o saldo de caixa do estúdio corrente (total de entradas − total de saídas), com filtros opcionais `dateFrom` e `dateTo`. Retorna `totalEntries`, `totalOutflows` e `balance`. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST` (EPIC-03-S13).
- **Projeção**: `GET /finance/cash-flow/entries/projection` — projeta o caixa disponível do estúdio corrente em um horizonte futuro (padrão 30 dias, configurável via `horizonDate`): saldo atual + recebíveis em aberto a vencer no horizonte − saídas futuras planejadas. Retorna `currentBalance`, `projectedEntries`, `projectedOutflows`, `projectedBalance` e `horizonDate`. Acessível a `OWNER`, `ADMIN`, `FINANCIAL` e `RECEPTIONIST` (EPIC-03-S14).
- **Multi-tenant**: movimentos de caixa sempre restritos ao estúdio corrente.
- **Persistência**: tabela `corely.cash_flow_entries` (migration `V10__cash_flow_entries.sql`).

## Testes

```bash
./mvnw test
```
