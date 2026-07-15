# STORY-021 — Sessões de Aula (ClassSession)

## Objetivo
Criar a entidade que representa uma ocorrência real de uma aula em uma data específica.

## Escopo
- `SessionStatus` — enum: SCHEDULED, FINISHED, CANCELLED
- `ClassSession` — entidade
- `ClassSessionRepository` — repositório
- `ClassSessionService` — serviço CRUD
- `ClassSessionController` — endpoints REST
- `ClassSessionRequest` / `ClassSessionResponse` — DTOs
- Migration V42 — tabela `comercial_class_sessions`
- Swagger (já configurado via `/comercial/**`)
- Testes unitários, de controller e de isolamento de tenant
- Documentação

## Entidade
Campos:
- id (UUID, herdado de BaseEntity)
- studio (herdado de ComercialBaseEntity)
- scheduleSlot (ManyToOne ScheduleSlot, obrigatório)
- sessionDate (DATE, obrigatório)
- startTime (TIME, obrigatório)
- endTime (TIME, obrigatório)
- capacity (INTEGER, obrigatório)
- bookedCount (INTEGER, default 0)
- status (VARCHAR, enum SessionStatus, default SCHEDULED)
- active (BOOLEAN, default true)
- createdAt (herdado de BaseEntity)
- updatedAt (herdado de BaseEntity)

## Regras
- Cada sessão pertence a um único ScheduleSlot
- Capacidade inicial herdada do ScheduleSlot no momento da criação
- bookedCount inicia em zero
- Não permitir sessões duplicadas para mesmo ScheduleSlot e mesma data
- Permitir alteração apenas enquanto status = SCHEDULED
- Exclusão lógica
- Multi-tenant obrigatório

## Endpoints
| Método | Path | Descrição |
|--------|------|-----------|
| POST | /comercial/class-sessions | Criar sessão |
| GET | /comercial/class-sessions | Listar sessões (paginado) |
| GET | /comercial/class-sessions/{id} | Buscar por ID |
| PUT | /comercial/class-sessions/{id} | Atualizar sessão |
| DELETE | /comercial/class-sessions/{id} | Excluir logicamente |

## Segurança
- OWNER, ADMIN, RECEPTIONIST: CRUD completo
- FINANCIAL: apenas consulta (GET)

## Banco
- Migration V42
- UNIQUE(schedule_slot_id, session_date)
- Índices em schedule_slot_id, session_date, status

## Fora do Escopo
- Booking
- Lista de espera
- Presença
- Instrutor
- Reposição
- Geração automática de sessões

## Critérios de Aceite
- CRUD funcionando
- Não permitir duplicidade
- Herdar capacidade do ScheduleSlot
- bookedCount inicial igual a zero
- Exclusão lógica
- Multi-tenant
- Swagger atualizado
- Testes automatizados passando
- Documentação atualizada
