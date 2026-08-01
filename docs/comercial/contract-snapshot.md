# Módulo Comercial — Contract Snapshot & StudentPlanResponse

## 1. Arquitetura de interpretação do ContractSnapshot

O `ContractSnapshot` persiste as regras do plano no momento da matrícula como
uma string JSON no campo `rules`.

Para evitar acoplamento entre o formato interno do JSON e o restante do sistema,
toda interpretação do snapshot está centralizada em dois componentes do pacote
`br.com.corely.comercial.contractsnapshot`:

- `ContractSnapshotParser`: desserializa o JSON, normaliza as chaves e devolve
  um objeto de domínio fortemente tipado.
- `ContractSnapshotData`: value object imutável que expõe as propriedades
  interpretadas (sem expor o JSON cru).

### 1.1 Regra de ouro

Nenhum `Service`, `Mapper` ou `Controller` deve:

- ler o campo `rules` diretamente;
- usar `ObjectMapper`, `JsonNode`, `Map<String,Object>` ou `TypeReference`;
- conhecer a estrutura JSON do snapshot.

Toda interpretação passa obrigatoriamente por `ContractSnapshotParser.parse(...)`.

### 1.2 Responsabilidades

| Componente | Responsabilidade |
| :--- | :--- |
| `ContractSnapshotParser` | Desserializar, validar estrutura, normalizar chaves, extrair propriedades. |
| `ContractSnapshotData` | Expor value object tipado (weeklyClasses, billingCycle, validityDays, autoRenew). |

## 2. Formato do snapshot e evolução de schema

### 2.1 Formato atual (flat)

```json
{
  "WEEKLY_CLASSES": 3,
  "VALIDITY_DAYS": 30,
  "BILLING_CYCLE": "MONTHLY",
  "AUTO_RENEW": true
}
```

### 2.2 Formato futuro suportado (nested)

```json
{
  "rules": {
    "weeklyClasses": 3,
    "validityDays": 30,
    "billingCycle": "MONTHLY",
    "autoRenew": true
  }
}
```

### 2.3 Normalização de chaves

O parser normaliza as chaves removendo caracteres não alfanuméricos e
convertendo para maiúsculas. Assim, `WEEKLY_CLASSES`, `weeklyClasses` e
`weekly_classes` são tratados como a mesma regra (`WEEKLYCLASSES`).

### 2.4 Proteção contra evolução

A evolução do schema do snapshot impacta **somente** o `ContractSnapshotParser`.
O restante do sistema consome apenas `ContractSnapshotData`.

## 3. StudentPlanResponse — Origem e nullability

| Campo | Origem | Pode ser null? | Regra de fallback |
| :--- | :--- | :--- | :--- |
| `planId` | `ContractSnapshot.planId` | Não | — |
| `planDescription` | `ContractSnapshot.planDescription` | Sim (plano sem descrição) | null |
| `planPrice` | `ContractSnapshot.planPrice` | Não | — |
| `billingCycle` | `BillingSchedule.frequency` (ativo) | Sim (sem billing schedule ativo) | null |
| `weeklyClasses` | Regra `WEEKLY_CLASSES` do snapshot | Sim (regra ausente ou inválida) | null |
| `nextBillingDate` | `BillingSchedule.nextBillingDate` (ativo) | Sim (sem billing schedule ativo) | null |
| `nextBillingFrequency` | `BillingSchedule.frequency` (ativo) | Sim (sem billing schedule ativo) | null |
| `nextBillingDay` | `BillingSchedule.billingDay` (ativo) | Sim (sem billing schedule ativo) | null |
| `nextBillingActive` | `BillingSchedule.active` (ativo) | Sim (sem billing schedule ativo) | null |

Regras de fallback:

- `billingCycle` e os campos `nextBilling*` são populados somente quando existe
  um `BillingSchedule` ativo vinculado à matrícula.
- `weeklyClasses` retorna null quando a regra não existe no snapshot ou possui
  valor não numérico.

## 4. Modelagem do DTO — decisão arquitetural

Revisão da Task (Item 6): considerar encapsular os campos `nextBillingDate`,
`nextBillingFrequency`, `nextBillingDay` e `nextBillingActive` em um
`BillingScheduleSummaryResponse`.

**Decisão:** manter o contrato atual (DTO achatado) nesta versão para preservar
compatibilidade com o frontend que já consome os campos planos. A melhoria de
encapsulamento em um objeto `BillingScheduleSummaryResponse` deve ser avaliada
em uma futura versão da API (v2), pois alterar o contrato agora quebraria
consumidores existentes.

## 5. Performance

### 5.1 Consultas carregadas com EntityGraph

`StudentPlanRepository` usa `@EntityGraph(attributePaths = {"student", "contractSnapshot"})`
em todas as consultas de leitura para evitar N+1.

### 5.2 Batch lookup de BillingSchedules

`findAll()` consulta os billing schedules em lote via
`BillingScheduleRepository.findByStudentPlanIdIn(...)` (com
`@EntityGraph(attributePaths = {"studentPlan"})`), evitando uma consulta por
matrícula durante a serialização. Nenhuma consulta adicional é disparada pelo
mapper.
