---
name: security
description: Conhecimento reutilizavel de seguranca no Corely (JWT, RBAC, multi-tenant, protecao de endpoints, CORS, auditoria). Use ao implementar autenticacao, autorizacao ou revisar seguranca.
---

# Security

Conhecimento reutilizavel de seguranca no Corely.

## Autenticacao

- JWT com secret via variavel de ambiente (nunca versionado).
- Rotation de segredo e profile de producao.
- `PasswordEncoder` (bcrypt) para senhas.

## Autorizacao / RBAC

- Roles e permissions por usuario (`UserRole`: OWNER, etc.).
- Restringir endpoints por role quando o dominio exigir.
- Nunca confiar em `studioId` vindo do client para isolamento.

## Multi-tenant

- Isolamento por `studio_id` em toda consulta (ver skill `multi-tenant`).
- Nunca permitir acesso cruzado entre studios.

## Protecao de API

- Swagger/OpenAPI protegido em producao.
- Rate limiting e lockout de login (roadmap EPIC 02).
- CORS por ambiente.
- Seed apenas em dev.
- Auditoria LGPD.

## Convencoes

- Nunca logar ou commitar segredos/chaves.
- `@RestControllerAdvice` para erros sem vazar detalhes internos.
- Validar entradas com Bean Validation.
