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

## Testes

```bash
./mvnw test
```
