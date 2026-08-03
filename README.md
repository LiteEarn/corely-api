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

## Testes

```bash
./mvnw test
```
