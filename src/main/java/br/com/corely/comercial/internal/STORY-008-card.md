# STORY-008 — StudentPlan (Contrato do Aluno)

## Objetivo
Implementar o contrato comercial do aluno. O StudentPlan representa o vínculo entre um aluno e um ContractSnapshot, sem referenciar Plan diretamente.

## Escopo
- Entidade `StudentPlan` estendendo `ComercialBaseEntity`
- Enum `StudentPlanStatus` (ACTIVE, SUSPENDED, CANCELLED, FINISHED)
- `StudentPlanRepository`, `StudentPlanService`, `StudentPlanController`
- DTOs (`StudentPlanRequest`, `StudentPlanResponse`)
- Migration V32 com FKs para student e contract_snapshot
- Testes unitários e de integração
- Swagger (grupo comercial)

## Endpoints
```
POST   /comercial/student-plans           (OWNER, ADMIN, RECEPTIONIST)
GET    /comercial/student-plans            (OWNER, ADMIN, RECEPTIONIST, FINANCIAL)
GET    /comercial/student-plans/{id}       (OWNER, ADMIN, RECEPTIONIST, FINANCIAL)
PUT    /comercial/student-plans/{id}/cancel    (OWNER, ADMIN, RECEPTIONIST)
PUT    /comercial/student-plans/{id}/suspend   (OWNER, ADMIN)
PUT    /comercial/student-plans/{id}/reactivate (OWNER, ADMIN)
```

## Regras
- Um StudentPlan sempre referencia um ContractSnapshot (nunca Plan diretamente)
- A contratação cria automaticamente um ContractSnapshot via ContractSnapshotService
- Não permitir dois contratos ACTIVE para o mesmo aluno
- Cancelamento não remove registros (preserva histórico)
- Não permitir exclusão física
- Apenas contratos ACTIVE podem ser suspensos ou cancelados
- Apenas contratos SUSPENDED podem ser reativados

## Fora do Escopo
- Cobrança, Invoice, Payment, Renovação automática
- Presença, Agendamento, Frontend

## Dependências
- STORY-002 (RuleDefinition)
- STORY-003 (Plan)
- STORY-004 (PlanRule)
- STORY-006 (RuleEngine)
- STORY-007 (ContractSnapshot)
