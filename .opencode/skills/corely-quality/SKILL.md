---
name: corely-quality
description: Checklist obrigatorio de qualidade para qualquer implementacao no Corely (11 secoes: Arquitetura, Dominio, Multi-Tenant, Seguranca, Banco, Performance, API, Codigo, Testes, Documentacao, Qualidade). Use antes de concluir qualquer alteracao de codigo e em revisoes do corely-review.
---

# Checklist de Qualidade Corely

Checklist obrigatorio a ser verificado em **qualquer implementacao** antes de
considera-la concluida. E a fonte unica de qualidade do projeto (substituiu a
skill `pr-review`). Usado pelo `corely-dev` antes de entregar e pelo
`corely-review` como checklist obrigatorio de auditoria.

## 1. Arquitetura (DDD / Clean Architecture / SOLID)

- [ ] **S**: responsabilidade unica — cada classe tem um unico motivo para mudar.
- [ ] **O**: aberto/fechado — evoluir por extensao, nao modificacao.
- [ ] **L**: substituicao de Liskov — subclasses respeitam o contrato da base.
- [ ] **I**: segregacao de interfaces — interfaces pequenas e especificas.
- [ ] **D**: inversao de dependencia — depender de abstracoes, nao de detalhes.
- [ ] Regras de negocio isoladas no dominio/Services, nunca no Controller.
- [ ] Controllers finos (apenas expoem endpoints e delegam).
- [ ] Camadas dependem em direcao ao dominio; sem ciclos entre modulos.
- [ ] DTOs separados das entidades.
- [ ] Mapper isolando conversao entidade <-> DTO.
- [ ] Modulos seguem a estrutura de packages padrao (domain/repository/service/dto/controller).

## 2. Dominio

- [ ] Modelagem alinhada ao dominio de negocio (use `corely-domain`).
- [ ] Invariantes de negocio protegidos na camada de dominio.
- [ ] Regras nao duplicadas entre Services/modulos.

## 3. Multi-Tenant

- [ ] Multi-tenant respeitado: todo acesso filtra por `studioId`.
- [ ] Sem `findAll` sem filtro; sem acesso entre studios (IDOR).
- [ ] Tenant context propagado corretamente em toda a cadeia de chamadas.

## 4. Seguranca

- [ ] JWT/RBAC preservados; endpoints protegidos conforme o papel.
- [ ] Sem exposicao de segredos/credenciais.
- [ ] Validacao de entrada (Bean Validation) em todos os endpoints.
- [ ] Sem injecao (SQL/NoSQL/command); dados de entrada tratados.
- [ ] CORS/CSRF coerentes; sem endpoints publicos indevidos.

## 5. Banco / Persistencia

- [ ] Sem N+1: `@EntityGraph`, `JOIN FETCH` ou batch queries onde ha colecoes.
- [ ] Sem consultas escondidas em mappers/`toResponse`/lacos.
- [ ] Indices e FKs coerentes; cascade/`fetch` Lazy configurados corretamente.
- [ ] Transacoes e locks corretos (atomicidade/concordancia).
- [ ] Flyway: mudanca de schema so via migration no schema `corely.`.

## 6. Performance

- [ ] Sem consultas desnecessarias em loops.
- [ ] Paginacao em listagens grandes.
- [ ] Sem processamento redundante ou payloads inflados.

## 7. API / REST

- [ ] Nenhum contrato REST existente alterado/removido.
- [ ] Swagger/OpenAPI coerente com a implementacao (`@Operation`, `@Schema` quando aplicavel).
- [ ] DTOs de resposta documentados com origem e nullability dos campos.
- [ ] Status HTTP e nomenclatura de endpoints conforme convencoes.

## 8. Codigo

- [ ] Sem codigo morto, TODO/FIXME, hardcode ou magic numbers.
- [ ] Sem duplicacao de logica; reuso de Services existentes.
- [ ] Nomes claros; metodos curtos; complexidade desnecessaria evitada.

## 9. Testes

- [ ] Teste unitario para cada Service com logica (JUnit + Mockito).
- [ ] Teste unitario para parsers/mappers com comportamento relevante.
- [ ] Teste de integracao para endpoints/repositories quando aplicavel.
- [ ] Testes cobrem cenario de sucesso, erro e bordas (null, vazio, nao encontrado, regra violada).
- [ ] Cobertura nao diminuiu em relacao ao estado anterior.
- [ ] Caminhos criticos (regra de negocio) cobertos.

## 10. Documentacao

- [ ] Endpoints documentados (OpenAPI/Swagger).
- [ ] Contrato OpenAPI coerente com a implementacao.
- [ ] `docs/`, README e roadmap atualizados.

## 11. Qualidade geral

- [ ] Nenhum TODO, codigo morto ou divida tecnica introduzida.
- [ ] Nenhuma logica duplicada.
- [ ] Entrega aderente ao escopo da Story (nada fora do roadmap).
- [ ] Nenhum arquivo fora da lista permitida alterado.
- [ ] Relatorio tecnico gerado.

## Classificacao de achados

| Severidade | Significado | Acao |
| :--- | :--- | :--- |
| **CRITICAL** | Impede PR: seguranca, multi-tenant, contrato REST quebrado | Bloqueia a aprovacao |
| **HIGH** | Corrigir antes de aprovar (regra de negocio, N+1, teste que nao valida) | Corrigir antes do `APPROVED` |
| **MEDIUM** | Corrigir antes da proxima release | Pode aprovar com registro |
| **LOW** | Melhoria opcional | Registrar, nao bloqueia |

Apenas achados `MEDIUM`/`LOW` permitem `APPROVED`. Qualquer `CRITICAL`/`HIGH` nao
resolvido => `CHANGES_REQUESTED`.
