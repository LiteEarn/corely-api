---
name: rest-api
description: Conhecimento reutilizavel de REST API no Corely (contratos, DTOs, OpenAPI/Swagger, status HTTP, versionamento). Use ao criar ou revisar endpoints.
---

# REST API

Conhecimento reutilizavel de APIs REST no Corely.

## Contratos

- **Nunca quebrar contratos REST existentes** (convencao do AI_CONTEXT.md).
- Ao evoluir um contrato, preferir adicionar campos opcionais ou versionar a API.
- DTOs separados para request e response.
- Respostas com campos null quando a origem nao disponibiliza valor (documentar via `@Schema`).

## Convencoes de endpoint

- Paths em portugues ou ingles consistente por modulo (ex.: `/comercial/plans`, `/comercial/student-plans`).
- HTTP verbs semanticos: GET para leitura, POST para criacao, PUT/PATCH para atualizacao, DELETE para remocao.
- `@Operation`, `@Tag`, `@Schema` para documentacao OpenAPI/Swagger.

## Regras

- Regra de negocio nunca no Controller; sempre no Service.
- Nao acessar Repository direto pelo Controller.
- Validar request com Bean Validation (`@Valid`).
- Erros padronizados via `@RestControllerAdvice`.

## Campos do dominio

- Documentar origem e nullability dos campos de response (ex.: `billingCycle` null quando nao ha billing schedule ativo).
- Preferir encapsular grupos de campos relacionados em sub-DTOs quando o contrato permitir (avaliar impacto de compatibilidade).
