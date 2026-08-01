---
description: Fluxo automatico de Go Live. Executa repetidamente o fluxo de /continue (implementar -> revisar -> aprovar) enquanto existir Story desbloqueada no roadmap e aprovada pelo @corely-review. Para ao encontrar bloqueador, quando uma story depende de aprovacao humana (IN_REVIEW) ou quando o reviewer reprova sem possibilidade de correcao na story. Gera relatorio.
agent: corely-dev
---

Voce e o agente **corely-dev**. Execute o fluxo automatico de **Go Live** do Corely com **aprovacao obrigatoria do @corely-review**.

## Fluxo

**Enquanto existir Story desbloqueada e aprovada:**

1. **Executar `/continue`**: execute o fluxo completo de `/continue` (ler PROJECT_STATUS → ler roadmap → localizar story atual → verificar dependencias → implementar → testar → **entregar para @corely-review** → aguardar veredito → se `APPROVED`, marcar `IN_REVIEW` → atualizar PROJECT_STATUS).
2. **Aguardar aprovacao humana**: uma story aprovada pelo reviewer fica `IN_REVIEW` aguardando aprovacao humana para virar `DONE`. O fluxo automatico **nao avanca** para a proxima story enquanto a Story Atual estiver `IN_REVIEW` — aguarde a aprovacao humana.
3. **Verificar bloqueio**: apos cada execucao de `/continue`, verifique se ainda existe uma proxima story desbloqueada (leia `PROJECT_STATUS.md` e o roadmap).

**Quando encontrar bloqueador:**

- Pare imediatamente.
- Gere relatorio final com: itens concluidos nesta execucao, bloqueador encontrado (story/dependencia/epico/`CHANGES_REQUESTED`/`IN_REVIEW`) e o que falta para desbloquear.

## Regras

- **Nunca implementar funcionalidades fora do roadmap.**
- **Nunca pular stories**: siga estritamente a ordem definida por `PROJECT_STATUS.md` e pelas dependencias (`dependsOn`).
- **Nunca avancar story `IN_REVIEW`**: uma story aprovada pelo reviewer depende de aprovacao humana antes de virar `DONE` e liberar a proxima.
- **Nunca emita `APPROVED`/`CHANGES_REQUESTED`**: veredito exclusivo do `corely-review`.
- **Nunca marque Story como `DONE`**: apenas `IN_REVIEW` apos aprovacao do reviewer; `DONE` somente apos aprovacao humana.
- Nunca quebre contratos REST existentes.
- Nunca crie divida tecnica, TODO, codigo morto ou hardcode.
- Respeite multi-tenant (`studioId`).
- Sempre atualize testes, documentacao, roadmap e PROJECT_STATUS a cada story.

## Bloqueadores que interrompem o fluxo

- Story com dependencia (`dependsOn`) ainda nao `DONE`.
- **Story `IN_REVIEW`** (aprovada pelo reviewer aguardando aprovacao humana).
- **`CHANGES_REQUESTED`** do reviewer com correcao fora do escopo da story (requer decisao).
- Decisao de arquitetura pendente (consulte `corely-architect`).
- Problema de seguranca ou multi-tenant nao resolvido.
- Falha de teste/compilacao que nao pode ser corrigida dentro da story.
