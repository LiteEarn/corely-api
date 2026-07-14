# STORY-020 — Horários da Agenda

## Objetivo
Permitir que cada Agenda possua horários configuráveis.

## Escopo
- `ScheduleSlot` — entidade
- `ScheduleSlotRepository` — repositório
- `ScheduleSlotService` — serviço CRUD
- `ScheduleSlotController` — endpoints REST
- `ScheduleSlotRequest` / `ScheduleSlotResponse` — DTOs
- Migration V41 — tabela `comercial_schedule_slots`
- Swagger (já configurado via `/comercial/**`)
- Testes unitários, de controller e de isolamento de tenant
- Documentação

## Entidade
Campos:
- id (UUID, herdado de BaseEntity)
- studio (herdado de ComercialBaseEntity)
- schedule (ManyToOne Schedule, obrigatório)
- dayOfWeek (VARCHAR, obrigatório) — MONDAY, TUESDAY, etc.
- startTime (TIME, obrigatório)
- endTime (TIME, obrigatório)
- capacity (INTEGER, obrigatório)
- active (BOOLEAN, default true)
- createdAt (herdado de BaseEntity)
- updatedAt (herdado de BaseEntity)

## Regras
- Cada Schedule possui vários horários
- Não permitir horários sobrepostos dentro da mesma Schedule e mesmo dia da semana
- endTime deve ser maior que startTime
- capacity deve ser maior que zero
- Exclusão lógica (active=false)
- Multi-tenant obrigatório

## Endpoints
| Método | Path | Descrição |
|--------|------|-----------|
| POST | /comercial/schedules/{scheduleId}/slots | Criar horário |
| GET | /comercial/schedules/{scheduleId}/slots | Listar horários da agenda |
| GET | /comercial/schedule-slots/{id} | Buscar horário por ID |
| PUT | /comercial/schedule-slots/{id} | Atualizar horário |
| DELETE | /comercial/schedule-slots/{id} | Excluir logicamente |

## Segurança
- OWNER, ADMIN, RECEPTIONIST: CRUD completo
- FINANCIAL: apenas consulta (GET)

## Banco
- Migration V41
- Índice em schedule_id
- Índice composto (schedule_id, day_of_week)

## Fora do Escopo
- Aulas
- Recorrência
- Reservas
- Instrutores
- Lista de espera
- Presença

## Critérios de Aceite
- CRUD funcionando
- Validação de conflito de horários
- Exclusão lógica
- Multi-tenant
- Swagger atualizado
- Testes automatizados passando
- Documentação atualizada
