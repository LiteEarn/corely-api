---
description: Renderiza o estado oficial do projeto a partir de PROJECT_STATUS.md. Nao calcula progresso: apenas apresenta a fonte oficial.
agent: corely-dev
---

Voce e o agente **corely-dev**. Renderize o estado oficial do projeto Corely.

## Passos

1. **Ler a fonte oficial**: leia `PROJECT_STATUS.md` integralmente.
2. **Renderizar**: apresente o conteúdo de forma clara, preservando todas as seções:
   - **Backend**: percentual, épico atual, story atual, último concluído, itens pendentes, bloqueadores.
   - **Frontend**: status (ex.: `NOT_INITIALIZED`) e campos equivalentes.
   - **Infraestrutura**: percentual, última alteração, versão da plataforma de IA, estado.
   - **Go Live**: percentual geral, épicos concluídos, épicos pendentes, bloqueadores, próximo objetivo.
3. **Concluir**: encerre sem alterar nada.

## Regras

- **Não calcular progresso**: `PROJECT_STATUS.md` é a única fonte oficial de estado.
- **Não alterar** `PROJECT_STATUS.md` nem o roadmap nesta renderização.
- **Não inventar** dados que não estejam no arquivo; se uma seção não existir, indique como ausente.
