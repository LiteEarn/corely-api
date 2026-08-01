---
name: ddd
description: Conhecimento reutilizavel de DDD e Clean Architecture para o Corely (bounded contexts, agregados, value objects, camadas). Use ao modelar dominio ou implementar novos modulos.
---

# DDD

Conhecimento reutilizavel de Domain-Driven Design no Corely.

## Bounded Contexts

- Modulos do Corely sao dominios isolados (ex.: `comercial`, `financialdashboard`, `studentplan`, `contractsnapshot`, `plan`).
- Cada modulo possui pacotes: `domain`/entities, `repository`, `service`, `dto`, `controller`.

## Elementos de dominio

- **Entidades**: objetos com identidade e ciclo de vida (ex.: `StudentPlan`, `ContractSnapshot`, `Plan`).
- **Value Objects**: objetos imutaveis e fortemente tipados (ex.: `ContractSnapshotData` interpretado a partir do JSON de regras).
- **Agregados**: cluster de entidades com invariantes (ex.: matricula + snapshot + billing schedule).
- **Repositories**: abstracao de persistencia por agregado.

## Regras

- Regra de negocio no dominio/Services, nunca no Controller nem no Frontend.
- Interpretacao de estruturas internas (JSON) centralizada em componentes dedicados (ex.: `ContractSnapshotParser`), nunca espalhada em Services.
- Baixo acoplamento, alta coesao.
- Modelar o contrato de acordo com o dominio (DTOs ricos quando fizer sentido), preservando compatibilidade.

## Fluxo de implementacao

1. Modelar entidades e value objects.
2. Definir invariantes e regras de negocio.
3. Implementar repository.
4. Implementar service com a logica de dominio.
5. Expor via DTO/Controller.
6. Testar comportamento do dominio.
