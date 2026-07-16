# STORY-026 — Reposição de Aula (MakeUp Classes)

## Objetivo
Permitir que um aluno utilize uma aula perdida para agendar uma reposição futura.

## Escopo
- `MakeUpCreditStatus` — enum: AVAILABLE, USED, EXPIRED, CANCELLED
- `MakeUpCredit` — entidade
- `MakeUpRepository` — repositório
- `MakeUpService` — serviço com geração automática via evento de domínio
- `MakeUpController` — endpoints REST
- `MakeUpProperties` — configuração de expiração (30 dias)
- `ClassSessionFinishedEvent` — evento de domínio disparado ao finalizar sessão
- `MakeUpCreditRequest` / `MakeUpCreditResponse` — DTOs
- Migration V46 — tabela `comercial_makeup_credits`
- Swagger (já configurado via `/comercial/**`)
- Testes unitários, de controller e de isolamento de tenant
- Documentação

## Entidade
Campos:
- id (UUID, herdado de BaseEntity)
- studio (herdado de ComercialBaseEntity)
- student (ManyToOne Student, obrigatório)
- originalAttendance (ManyToOne Attendance, obrigatório)
- originalClassSession (ManyToOne ClassSession, obrigatório)
- makeUpBooking (ManyToOne Booking, nullable)
- expirationDate (DATE, obrigatório)
- status (VARCHAR, enum MakeUpCreditStatus, default AVAILABLE)
- reason (VARCHAR 500)
- active (BOOLEAN, default true)
- createdAt (herdado de BaseEntity)
- updatedAt (herdado de BaseEntity)

## MakeUpCreditStatus
- AVAILABLE
- USED
- EXPIRED
- CANCELLED

## Geração do crédito
- Criado automaticamente quando Attendance = ABSENT e aula está FINISHED
- Disparado via `ClassSessionFinishedEvent` (evento de domínio)
- Não gera crédito para PRESENT ou EXCUSED

## Utilização
- Crédito AVAILABLE utilizado apenas uma vez
- Cria Booking CONFIRMED via BookingService (reutiliza fluxo oficial)
- Vincula o Booking ao crédito
- Altera status para USED

## Validações
- Não permitir utilizar crédito expirado, cancelado, usado ou inativo
- Não permitir utilizar crédito para aula já iniciada, finalizada ou cancelada

## Expiração
- Configurável via `corely.makeup.expiration-days` (default 30 dias)
- Armazenada na entidade (expirationDate)

## Endpoints
| Método | Path | Descrição |
|--------|------|-----------|
| GET | /comercial/makeup-credits | Listar créditos (paginado) |
| GET | /comercial/makeup-credits/{id} | Buscar por ID |
| POST | /comercial/makeup-credits/{id}/use | Utilizar crédito |

## Segurança
- OWNER, ADMIN, RECEPTIONIST: CRUD operacional
- FINANCIAL: somente leitura (GET)

## Banco
- Migration V46
- Índices em student_id, status, expiration_date

## Dependências
- STORY-023 (Attendance entity)
- STORY-025 (ClassSession lifecycle - finish)

## Critérios de Aceite
- Crédito criado automaticamente ao finalizar sessão com ABSENT
- Crédito utilizado apenas uma vez
- Booking reutiliza fluxo oficial do BookingService
- Expiração funcionando
- Multi-tenant preservado
- Swagger atualizado
- Testes automatizados passando
- Documentação atualizada

## Status
Completed
