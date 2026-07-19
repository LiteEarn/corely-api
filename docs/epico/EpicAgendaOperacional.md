# Épico: Agenda Operacional (Scheduling)

**Produto:** Corely — SaaS de gestão de Studios de Pilates
**Responsável pela quebra:** Product Owner / Arquitetura
**Consumidor deste documento:** Agente de desenvolvimento (OpenCode)

---

## Nota de Contexto (leitura obrigatória antes de iniciar qualquer história)

O Corely já possui hoje, implementados, os conceitos de **Turma**, **Sessão de Aula**, **Registro de Presença** e **Solicitação de Reposição**. Este backlog descreve a Agenda Operacional em termos de negócio (Class Session e Booking), conforme solicitado. Cabe à arquitetura, no momento da implementação de cada história, avaliar se ela representa uma capacidade nova ou uma evolução de algo que já existe no sistema — este documento não toma essa decisão, apenas descreve o valor de negócio esperado.

---

## Domínio de Referência

- **Class Session**: uma aula agendada (turma, instrutor, sala, data, horário, capacidade, status). Existe mesmo sem alunos reservados.
- **Booking**: a reserva de um aluno específico em uma Class Session. Cada aluno tem seu próprio booking, com status próprio (Agendado, Confirmado, Presente, Falta, Cancelado, Reposição). Uma Class Session tem vários Bookings.

---

## Backlog de Histórias

---

### HISTÓRIA 1 — Criar Sessão de Aula

**Objetivo:** Permitir que o studio registre uma aula que vai acontecer, definindo turma, instrutor, sala, data, horário e capacidade máxima de alunos.

**Valor para o usuário:** A recepção e os instrutores passam a enxergar, de forma confiável, quais aulas estão programadas para acontecer — é a base de toda a operação diária do studio.

**Critérios de Aceite:**
- É possível criar uma sessão informando turma, instrutor, sala, data, horário e capacidade.
- Uma sessão pode existir mesmo sem nenhum aluno reservado.
- A sessão criada nasce em um status inicial que representa "programada/agendada".
- Não é possível criar uma sessão com informações obrigatórias ausentes (ex.: sem instrutor, sem sala, sem data/horário).

**Dependências:** Nenhuma (turma, instrutor e sala já existem como módulos do Corely).

**Complexidade:** P

---

### HISTÓRIA 2 — Editar Sessão de Aula

**Objetivo:** Permitir corrigir ou ajustar informações de uma sessão já criada (instrutor, sala, horário, capacidade) antes de ela acontecer.

**Valor para o usuário:** Studios precisam corrigir erros de cadastro e fazer pequenos ajustes de última hora (troca de instrutor, troca de sala) sem precisar recriar a aula do zero.

**Critérios de Aceite:**
- É possível alterar instrutor, sala, horário e capacidade de uma sessão existente.
- Uma sessão que já aconteceu (passada) ou já foi cancelada não pode ser editada.
- Alterações que reduzam a capacidade abaixo do número de alunos já reservados são impedidas.

**Dependências:** História 1.

**Complexidade:** P

---

### HISTÓRIA 3 — Cancelar Sessão de Aula

**Objetivo:** Permitir que o studio cancele uma aula que não vai mais acontecer (ex.: feriado não planejado, instrutor doente).

**Valor para o usuário:** Evita que alunos cheguem para uma aula que não vai ocorrer e dá visibilidade imediata do cancelamento para toda a operação.

**Critérios de Aceite:**
- É possível cancelar uma sessão futura, informando o motivo do cancelamento.
- Uma sessão cancelada muda de status e deixa de aceitar novas reservas.
- Todos os alunos que tinham reserva ativa nessa sessão são identificados como impactados pelo cancelamento.
- Uma sessão que já aconteceu não pode ser cancelada retroativamente.

**Dependências:** História 1.

**Complexidade:** P

---

### HISTÓRIA 4 — Reagendar Sessão de Aula

**Objetivo:** Permitir mover uma sessão já criada para outra data/horário, mantendo o histórico de que ela foi reagendada.

**Valor para o usuário:** Studios frequentemente precisam mudar o horário de uma aula pontual sem perder as reservas já feitas pelos alunos.

**Critérios de Aceite:**
- É possível alterar a data/horário de uma sessão futura.
- As reservas (Bookings) já existentes nessa sessão são preservadas após o reagendamento.
- Fica registrado que a sessão foi reagendada (não se confunde com uma sessão nova).
- Não é possível reagendar uma sessão já cancelada ou já ocorrida.

**Dependências:** Histórias 1 e 2.

**Complexidade:** M

---

### HISTÓRIA 5 — Configurar Recorrência de Sessões

**Objetivo:** Permitir que o studio defina uma aula que se repete automaticamente em determinados dias da semana e horário, sem precisar cadastrar cada ocorrência manualmente.

**Valor para o usuário:** A grande maioria das aulas de um studio de Pilates é recorrente (ex.: toda segunda às 8h); cadastrar isso manualmente todo dia é inviável operacionalmente.

**Critérios de Aceite:**
- É possível definir uma recorrência (dias da semana, horário, turma, instrutor, sala, capacidade) associada a uma turma.
- A partir da recorrência configurada, sessões futuras são geradas automaticamente respeitando o padrão definido.
- É possível interromper uma recorrência sem afetar as sessões já geradas no passado.
- Alterar a recorrência não apaga sessões já reservadas por alunos.

**Dependências:** História 1.

**Complexidade:** G

---

### HISTÓRIA 6 — Bloquear Agenda de Instrutor ou Sala

**Objetivo:** Permitir registrar que um instrutor ou uma sala está indisponível em um período (folga, manutenção, feriado, férias), impedindo que novas sessões sejam criadas nesse período.

**Valor para o usuário:** Evita que a recepção agende aulas em horários que na prática não podem acontecer, reduzindo retrabalho e cancelamentos de última hora.

**Critérios de Aceite:**
- É possível registrar um bloqueio de agenda para um instrutor ou para uma sala, com data/período de início e fim e um motivo.
- Não é possível criar uma nova sessão que colida com um bloqueio ativo.
- É possível consultar quais bloqueios estão vigentes em um determinado período.
- Sessões já existentes antes da criação de um bloqueio não são canceladas automaticamente — o bloqueio afeta apenas novos agendamentos.

**Dependências:** Histórias 1 e 2.

**Complexidade:** M

---

### HISTÓRIA 7 — Consultar Disponibilidade de Horários

**Objetivo:** Permitir que a recepção veja rapidamente quais horários, salas e instrutores estão livres em um determinado dia, antes de criar ou remarcar uma sessão.

**Valor para o usuário:** Acelera o atendimento ao aluno e evita erros de agendamento (tentar marcar em horário já ocupado), sem precisar cruzar informações manualmente.

**Critérios de Aceite:**
- É possível consultar, para uma data e um recurso (instrutor ou sala), quais horários já estão ocupados e quais estão livres.
- A consulta considera sessões já criadas e bloqueios de agenda vigentes.
- O resultado é apresentado de forma clara para apoiar a decisão de quando agendar.

**Dependências:** Histórias 1 e 6.

**Complexidade:** M

---

### HISTÓRIA 8 — Detectar Conflitos de Agenda

**Objetivo:** Impedir automaticamente que o mesmo instrutor ou a mesma sala fiquem escalados em duas sessões com horários sobrepostos.

**Valor para o usuário:** Elimina um erro operacional clássico e recorrente em studios (dupla marcação de instrutor/sala), que gera confusão e insatisfação de alunos.

**Critérios de Aceite:**
- Ao criar, editar ou reagendar uma sessão, o sistema identifica se há sobreposição de horário para o mesmo instrutor ou mesma sala.
- Uma tentativa de gerar conflito é bloqueada, com uma mensagem explicando o motivo.
- A verificação de conflito também se aplica às sessões geradas por recorrência (História 5).

**Dependências:** Histórias 1, 2, 4 e 5.

**Complexidade:** M

---

### HISTÓRIA 9 — Reservar Vaga do Aluno em uma Sessão (Booking)

**Objetivo:** Permitir que um aluno seja reservado individualmente em uma Class Session específica.

**Valor para o usuário:** É o ato central da operação diária — sem isso, a agenda existe mas nenhum aluno pode efetivamente frequentar uma aula.

**Critérios de Aceite:**
- É possível reservar um aluno em uma sessão futura, criando um Booking individual para ele.
- Um aluno não pode ter duas reservas ativas na mesma sessão.
- A reserva nasce em status "Agendado".
- Não é possível reservar um aluno em uma sessão cancelada ou já ocorrida.

**Dependências:** História 1.

**Complexidade:** M

---

### HISTÓRIA 10 — Controlar Capacidade da Sessão

**Objetivo:** Impedir que uma sessão receba mais reservas do que sua capacidade máxima permite.

**Valor para o usuário:** Evita superlotação da sala e garante que a experiência da aula (equipamento, espaço, atenção do instrutor) seja preservada.

**Critérios de Aceite:**
- Ao tentar reservar um aluno em uma sessão que já atingiu a capacidade máxima, a reserva é impedida e o motivo é informado.
- Cancelamentos de Bookings liberam vaga imediatamente para novas reservas.
- A capacidade disponível de uma sessão pode ser consultada a qualquer momento.

**Dependências:** Histórias 1 e 9.

**Complexidade:** P

---

### HISTÓRIA 11 — Confirmar Presença Prevista

**Objetivo:** Permitir que a reserva de um aluno seja marcada como "Confirmada", sinalizando que ele efetivamente pretende comparecer.

**Valor para o usuário:** Dá ao studio uma visão mais confiável de quantos alunos realmente vão aparecer, diferente de apenas "quem reservou".

**Critérios de Aceite:**
- Uma reserva em status "Agendado" pode ser alterada para "Confirmado".
- Não é possível confirmar uma reserva que já foi cancelada.
- O histórico de mudança de status da reserva fica disponível para consulta.

**Dependências:** História 9.

**Complexidade:** P

---

### HISTÓRIA 12 — Cancelar Reserva do Aluno

**Objetivo:** Permitir que a reserva de um aluno específico em uma sessão seja cancelada, sem afetar a sessão em si ou as reservas de outros alunos.

**Valor para o usuário:** Alunos frequentemente precisam desistir de uma aula específica sem cancelar seu vínculo com o studio como um todo.

**Critérios de Aceite:**
- É possível cancelar um Booking individual, mudando seu status para "Cancelado".
- O cancelamento de um Booking libera a vaga na sessão (ver História 10).
- Não é possível cancelar um Booking que já foi marcado como "Presente" ou "Falta".
- Outros Bookings da mesma sessão não são afetados.

**Dependências:** Histórias 9 e 10.

**Complexidade:** P

---

### HISTÓRIA 13 — Registrar Presença ou Falta do Aluno

**Objetivo:** Permitir que, após a aula acontecer, cada Booking seja marcado como "Presente" ou "Falta".

**Valor para o usuário:** É o registro operacional que alimenta o acompanhamento de frequência do aluno e a gestão do studio sobre engajamento.

**Critérios de Aceite:**
- É possível marcar individualmente cada Booking de uma sessão como "Presente" ou "Falta".
- É possível marcar a presença de todos os alunos de uma sessão de uma vez (registro em lote).
- Só é possível registrar presença/falta para sessões já ocorridas (data/horário no passado) ou em andamento.
- Um Booking cancelado não pode receber marcação de presença/falta.

**Dependências:** Histórias 9 e 12.

**Complexidade:** M

---

### HISTÓRIA 14 — Marcar Reserva como Reposição

**Objetivo:** Permitir identificar que um Booking específico representa uma aula de reposição de um aluno (e não uma aula regular da sua rotina).

**Valor para o usuário:** Permite ao studio diferenciar, na agenda do dia, quem está numa aula "normal" e quem está repondo uma aula perdida — importante para controle de vagas e para o instrutor entender o contexto do aluno em sala.

**Critérios de Aceite:**
- É possível criar ou marcar um Booking com o status "Reposição", distinto de "Agendado".
- Um Booking de reposição respeita as mesmas regras de capacidade da sessão (História 10).
- É possível identificar, ao consultar uma sessão, quais Bookings são reposição e quais são regulares.

**Dependências:** Histórias 9 e 10.

**Complexidade:** P

---

### HISTÓRIA 15 — Dashboard Operacional do Dia

**Objetivo:** Apresentar, em uma única visão, todas as sessões do dia com sua ocupação (quantos alunos reservados vs. capacidade) e status.

**Valor para o usuário:** É a tela que a recepção e a gestão do studio usam a cada manhã para entender o dia — quantas aulas, quantas vagas ocupadas, quantas sobrando, se há algo cancelado.

**Critérios de Aceite:**
- É possível visualizar todas as sessões de um dia específico, com instrutor, sala, horário e ocupação (reservas ativas / capacidade).
- Sessões canceladas aparecem sinalizadas de forma distinta das demais.
- É possível ver rapidamente quais sessões estão com vagas disponíveis e quais estão lotadas.

**Dependências:** Histórias 1, 3, 9, 10.

**Complexidade:** M

---

## Ordem Recomendada de Implementação

1. História 1 — Criar Sessão de Aula
2. História 2 — Editar Sessão de Aula
3. História 3 — Cancelar Sessão de Aula
4. História 9 — Reservar Vaga do Aluno em uma Sessão (Booking)
5. História 10 — Controlar Capacidade da Sessão
6. História 12 — Cancelar Reserva do Aluno
7. História 11 — Confirmar Presença Prevista
8. História 13 — Registrar Presença ou Falta do Aluno
9. História 14 — Marcar Reserva como Reposição
10. História 4 — Reagendar Sessão de Aula
11. História 6 — Bloquear Agenda de Instrutor ou Sala
12. História 7 — Consultar Disponibilidade de Horários
13. História 8 — Detectar Conflitos de Agenda
14. História 5 — Configurar Recorrência de Sessões
15. História 15 — Dashboard Operacional do Dia

**Racional da ordem:** primeiro estabelece-se o ciclo de vida básico da Class Session (1→3), depois o ciclo de vida básico do Booking (9→14), pois é o fluxo que gera valor de negócio mais imediato e testável ponta a ponta (agendar aula → aluno reserva → aluno frequenta). Só então entram as capacidades de agenda mais avançadas (reagendamento, bloqueios, disponibilidade, conflitos, recorrência), que dependem do ciclo básico já estar sólido. O Dashboard Operacional fica por último por ser uma visão consolidada que depende de praticamente todas as capacidades anteriores existirem.