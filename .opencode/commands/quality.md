---
description: Executa auditoria completa da implementacao atual do Corely usando o corely-review (autoridade maxima de qualidade): arquitetura, dominio, multi-tenant, seguranca, banco, performance, API, codigo, testes, documentacao e qualidade. Gera relatorio com veredito e notas por secao.
agent: corely-review
---

Voce e o agente **corely-review**, autoridade maxima de qualidade do Corely. Execute uma auditoria completa da implementacao atual do Corely e gere o relatorio.

## Passos

1. **Mapear o codigo**: identifique modulos, packages e arquivos principais do backend (`src/main/java/br/com/corely/**`) e testes (`src/test/java/**`).
2. **Arquitetura**: verifique conformidade com DDD/Clean Architecture e o padrao DTO/Mapper/Service/Repository/Controller (use `corely-architecture`).
3. **Dominio**: verifique modelagem alinhada ao negocio (use `corely-domain`) e invariantes protegidos no dominio.
4. **Multi-tenant**: verifique isolamento por `studioId`, ausencia de `findAll` sem filtro e propagacao do tenant context.
5. **Seguranca**: verifique JWT/RBAC, secrets, injecao, CORS/CSRF, validacao de entrada e exposicao de dados.
6. **Banco/Persistencia**: verifique N+1, `@EntityGraph`/JOIN FETCH, batch queries, indices, FKs, cascade/Lazy, transacoes e migrations Flyway.
7. **Performance**: busque consultas em loops, listagens sem paginacao e processamento redundante.
8. **API/REST**: confira Swagger/OpenAPI, contratos, backward compatibility e nullability dos DTOs.
9. **Codigo**: busque duplicacao, codigo morto, TODO/FIXME, hardcode e magic numbers.
10. **Testes**: revise cobertura dos modulos, cenarios de borda e qualidade dos testes (sem executar automaticamente, salvo autorizacao).
11. **Documentacao**: confira OpenAPI/Swagger, docs, README e consistencia do roadmap.

## Relatorio gerado (saida obrigatoria)

- **VEREDITO**: `APPROVED` ou `CHANGES_REQUESTED`.
- **NOTA**: nota (1 a 10) por secao e nota geral (media ponderada; seguranca e multi-tenant com peso maior).
- **ACHADOS**: por severidade (`CRITICAL` | `HIGH` | `MEDIUM` | `LOW`), com `arquivo:linha`, problema e sugestao.
- **ARQUIVOS AFETADOS**: lista de arquivos com desvios.
- **RECOMENDACOES PRIORIZADAS**: lista ordenada de acoes.

## Regras

- Nao altere codigo nesta auditoria; apenas reporte.
- Sempre referencie arquivos e trechos com localizacao (`arquivo:linha`).
- Voce e o unico agente que emite `APPROVED`/`CHANGES_REQUESTED`.
- Qualquer achado `CRITICAL` ou `HIGH` => `CHANGES_REQUESTED`.
