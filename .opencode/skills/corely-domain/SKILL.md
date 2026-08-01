---
name: corely-domain
description: Conhecimento do dominio de negocio do Corely (aluno, matricula, plano, cobranca, agenda, presenca, evolucao, avaliacao, booking, financeiro). Nunca contem implementacao. Use ao entender contexto de negocio ou modelar novas funcionalidades.
---

# Domínio Corely

Conhecimento conceitual do domínio de negócio do Corely. Este documento contém
apenas conhecimento de domínio — nunca implementação, código ou detalhes de
persistência.

## Aluno

- Pessoa que frequenta o estúdio.
- Possui dados de contato, histórico de matrículas, presença e evolução.
- É o ator central de todos os fluxos de agenda, cobrança e avaliação.

## Matrícula

- Vínculo do aluno com um plano em um estúdio.
- Define o plano ativo, período de vigência e regras de cobrança.
- Pode ser renovada, cancelada ou suspensa conforme o fluxo de negócio.

## Plano

- Oferta de aulas/turmas com periodicidade definida.
- Possui regras de cobrança (ciclo, recorrência) e regras de uso (aulas por
  semana, validade).
- Base para a matrícula e para o financeiro.

## Cobrança

- Geração e controle de cobranças sobre a matrícula conforme o plano.
- Segue o ciclo de faturamento e o billing schedule da matrícula.
- Alimenta o financeiro do estúdio.

## Agenda

- Grade de turmas e horários do estúdio.
- Permite alocar instrutores, alunos e espaços.
- Base para presença e para o dashboard operacional.

## Presença

- Registro de comparecimento do aluno às aulas.
- Consome o saldo de aulas do plano.
- Alimenta indicadores operacionais e de aderência.

## Evolução

- Acompanhamento do progresso do aluno ao longo do tempo.
- Suportado por indicadores e métricas ao longo do ciclo do aluno.

## Avaliação

- Registro de avaliações do aluno (física, evolução, reavaliação).
- Complementa a evolução e o atendimento.

## Booking

- Reserva/agendamento de aulas ou horários pelo aluno.
- Interage com a agenda, o saldo de aulas e a presença.

## Financeiro

- Fluxo de recebimentos, cobranças e indicadores financeiros do estúdio.
- Unifica cobrança, matrícula e plano.
- Dashboard financeiro consome estes dados.

## Relações essenciais

- Aluno ↔ Matrícula ↔ Plano → Cobrança → Financeiro.
- Aluno → Agenda → Presença → Evolução.
- Booking liga Aluno e Agenda respeitando o saldo do Plano.
- Avaliação alimenta Evolução.

## Regras de uso

- Sempre consultar este contexto antes de modelar entidades ou fluxos.
- Não duplicar conceitos entre bounded contexts (ex.: cobrança no financeiro e
  billing schedule no comercial representam o mesmo conceito em contextos
  diferentes).
- Nunca misturar este conhecimento com implementação concreta do código.
