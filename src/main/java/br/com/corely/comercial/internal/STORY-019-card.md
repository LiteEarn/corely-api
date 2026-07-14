# STORY-019 — Estrutura Base da Agenda

## Objetivo
Criar a estrutura base da Agenda do Corely.

## Escopo
- `Schedule` — entidade
- `ScheduleRepository` — repositório
- `ScheduleService` — serviço CRUD
- `ScheduleController` — endpoints REST
- `ScheduleRequest` / `ScheduleResponse` — DTOs
- Migration V40 — tabela `comercial_schedules`
- Swagger (já configurado via `/comercial/**`)
- Testes unitários, de controller e de isolamento de tenant
- Documentação

## Entidade
Campos:
- id (UUID, herdado de BaseEntity)
- studio (herdado de ComercialBaseEntity)
- name (String, obrigatório)
- description (String, opcional)
- active (Boolean, default true)
- createdAt (herdado de BaseEntity)
- updatedAt (herdado de BaseEntity)

## Regras
- Cada Studio possui várias agendas
- Nome obrigatório
- Nome único por Studio (UNIQUE(studio_id, name))
- Exclusão lógica (active=false)
- Multi-tenant obrigatório

## Endpoints
| Método | Path | Descrição |
|--------|------|-----------|
| POST | /comercial/schedules | Criar agenda |
| GET | /comercial/schedules | Listar agendas (paginado) |
| GET | /comercial/schedules/{id} | Buscar por ID |
| PUT | /comercial/schedules/{id} | Atualizar agenda |
| DELETE | /comercial/schedules/{id} | Excluir logicamente |

## Segurança
- OWNER, ADMIN, RECEPTIONIST: CRUD completo
- FINANCIAL: apenas consulta (GET)

## Banco
- Migration V40
- Índice por studio_id
- UNIQUE(studio_id, name)

## Fora do Escopo
- Horários
- Aulas
- Instrutores
- Reservas
- Lista de espera
- Recorrência

## Critérios de Aceite
- CRUD funcionando
- Exclusão lógica
- Multi-tenant
- Swagger atualizado
- Testes automatizados passando
- Documentação atualizada
