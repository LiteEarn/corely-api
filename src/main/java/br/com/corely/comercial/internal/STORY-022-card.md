# STORY-022 — Reserva de Alunos (Booking)

## Objetivo
Permitir que um aluno reserve uma vaga em uma ClassSession.

## Escopo
- `BookingStatus` — enum: CONFIRMED, CANCELLED
- `Booking` — entidade
- `BookingRepository` — repositório
- `BookingService` — serviço CRUD com validações de negócio
- `BookingController` — endpoints REST
- `BookingRequest` / `BookingResponse` — DTOs
- Migration V43 — tabela `comercial_bookings`
- Swagger (já configurado via `/comercial/**`)
- Testes unitários, de controller e de isolamento de tenant
- Documentação

## Entidade
Campos:
- id (UUID, herdado de BaseEntity)
- studio (herdado de ComercialBaseEntity)
- classSession (ManyToOne ClassSession, obrigatório)
- student (ManyToOne Student, obrigatório)
- bookingDateTime (TIMESTAMP, obrigatório)
- status (VARCHAR, enum BookingStatus, default CONFIRMED)
- active (BOOLEAN, default true)
- createdAt (herdado de BaseEntity)
- updatedAt (herdado de BaseEntity)

## BookingStatus
- CONFIRMED
- CANCELLED

## Regras
- Cada aluno pode possuir apenas uma reserva por ClassSession (UNIQUE)
- Ao criar: validar Student ativo, validar StudentPlan ACTIVE, validar bookingBlocked = false, validar ClassSession ACTIVE, validar status = SCHEDULED, validar bookedCount < capacity
- Ao criar: incrementar bookedCount da ClassSession
- Ao cancelar: alterar status para CANCELLED, decrementar bookedCount (nunca negativo)
- Exclusão lógica
- Multi-tenant obrigatório
- Concorrência: create e delete utilizam findByIdWithLock na ClassSession

## Endpoints
| Método | Path | Descrição |
|--------|------|-----------|
| POST | /comercial/bookings | Criar reserva |
| GET | /comercial/bookings | Listar reservas (paginado) |
| GET | /comercial/bookings/{id} | Buscar por ID |
| DELETE | /comercial/bookings/{id} | Cancelar/excluir logicamente |

## Segurança
- OWNER, ADMIN, RECEPTIONIST: CRUD completo
- FINANCIAL: apenas consulta (GET)

## Banco
- Migration V43
- UNIQUE(class_session_id, student_id)
- Índices em class_session_id, student_id, status

## Fora do Escopo
- Lista de espera
- Check-in
- Presença
- Reposição
- Notificações
- WhatsApp

## Critérios de Aceite
- CRUD funcionando
- Não permitir duplicidade
- Não ultrapassar capacidade
- Atualizar bookedCount corretamente
- Cancelamento libera vaga
- Exclusão lógica
- Multi-tenant
- Swagger atualizado
- Testes automatizados passando
- Documentação atualizada
