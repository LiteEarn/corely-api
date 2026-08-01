---
name: release
description: Conhecimento reutilizavel para release, deploy e Go Live do Corely (Docker, CI/CD, ambientes, observabilidade, bloqueadores de Go Live). Use ao preparar ou executar publicacoes.
---

# Release

Conhecimento reutilizavel de release, deploy e Go Live no Corely.

## Bloqueadores de Go Live (do roadmap)

- **EPIC 01 — Multi-Tenant** (critico): isolamento por `studio_id`, sem `findAll` sem filtro.
- **EPIC 02 — Seguranca** (critico): JWT secret por env, profile producao, Swagger protegido, seed apenas em dev.
- Antes do Go Live: pentest, backup/restore, monitoramento, documentacao da API, manual operacional, staging e producao.

## Deploy

- Dockerfile para o backend Java 21.
- Docker Compose de producao.
- Configuracao por ambiente (`application.yaml`, `application-dev.yaml`, `application-prod.yaml`).
- Variaveis de ambiente para segredos (JWT secret, credenciais).

## CI/CD

- Pipeline de CI: build + testes.
- Pipeline de CD: build de imagem, push, deploy.
- Scan de dependencias e secrets.

## Observabilidade

- Micrometer + Prometheus.
- Health checks: readiness e liveness.
- Logs estruturados e TraceId.

## Operacao

- Backups e restores validados.
- Monitoramento ativo antes de publicar.
- Documentacao da API disponivel.
