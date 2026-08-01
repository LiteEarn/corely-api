---
name: corely-architecture
description: Descreve a arquitetura do Corely: modulos, packages, convencoes e o padrao DTO/Mapper/Repository/Service/Controller. Use ao implementar ou revisar codigo para garantir conformidade arquitetural.
---

# Arquitetura do Corely

Guia arquitetural para desenvolvimento no projeto Corely.

## Módulos

- O backend é organizado em **bounded contexts** (DDD). Cada contexto é um
  pacote raiz (ex.: `br.com.corely.comercial`, `br.com.corely.financialdashboard`).
- Contextos existentes: `comercial` (planos, matrículas, billing),
  `financialdashboard` (indicadores financeiros), `studentplan`,
  `contractsnapshot`, `plan` e demais domínios.

## Packages (padrão por módulo)

```
br.com.corely.<dominio>/
  domain/ (entidades)
  repository/
  service/
  dto/
  controller/
```

- Entidades de domínio com identidade e ciclo de vida.
- Repositories como abstração de persistência por agregado.
- Services com a lógica de negócio.
- DTOs como contrato de entrada/saída.
- Controllers finos, apenas expondo endpoints.

## Convenções

- Backend: Java 21, Spring Boot, Clean Architecture, Flyway, PostgreSQL.
- Frontend: Angular, Signals, Design System próprio, texto em português.
- Toda regra de negócio pertence ao backend; frontend só apresenta informação.
- Multi-tenant: todo dado pertence a um `Studio`; sempre respeitar `studioId`.

## Padrão DTO → Mapper → Service → Repository → Controller

Fluxo de uma requisição:

1. **Controller**: recebe request DTO, valida (Bean Validation), chama Service.
2. **Service**: orquestra regras de negócio, usa Mapper e Repository.
3. **Mapper**: converte entre entidade e DTO.
4. **Repository**: persiste/consulta no banco (Spring Data).

Nunca:

- Acessar Repository diretamente pelo Controller.
- Colocar regra de negócio no Controller.
- Duplicar lógica entre Services.
- Quebrar contratos REST existentes.

Interpretação de estruturas internas (ex.: JSON de snapshot) deve ser
centralizada em componentes dedicados (ex.: `ContractSnapshotParser`), nunca
espalhada em Services ou Controllers.

## Diretrizes de qualidade

- Evitar N+1: usar `@EntityGraph` ou joins quando carregar coleções.
- DTOs ricos quando fizer sentido, preservando compatibilidade de contrato.
- Testes unitários (Service, Parser) + integração (Repository, API).
- Manter baixo acoplamento e alta coesão entre camadas.
