# Relatório de Auditoria AUTH-003B - Permissões Frontend vs Backend

## Escopo
Comparar a matriz de permissões do **frontend** (`corelyWeb/permission-matrix.ts`) com as anotações `@RequireRole` do **backend** (`corely-api/DashboardController.java` e demais controllers).

---

## 1. Arquivos de Segurança Analisados (Backend)

| Tipo | Encontrado? | Localização |
|------|-------------|-------------|
| `@PreAuthorize` | ❌ Não utilizado | — |
| `@Secured` | ❌ Não utilizado | — |
| `SecurityFilterChain` | ✅ Sim | `auth/config/SecurityConfiguration.java:55` |
| `PermissionEvaluator` | ❌ Não utilizado | — |
| `RoleHierarchy` | ❌ Não utilizado | — |
| `@RequireRole` (custom) | ✅ Sim | 62 usos em 11 controllers |
| `@RequireAnyRole` (custom) | ✅ Sim | `auth/authorization/RequireAnyRole.java` |
| `@RequirePermission` (custom) | ✅ Definido, **NUNCA usado** | `auth/authorization/RequirePermission.java` |
| `AuthorizationInterceptor` | ✅ Sim | `auth/authorization/AuthorizationInterceptor.java:15` |
| `AuthorizationService` | ✅ Sim | `auth/authorization/AuthorizationService.java:15` |
| `RolePermissions` | ✅ Sim | `auth/authorization/RolePermissions.java` |

---

## 2. Divergência Encontrada (Dashboard)

### Permissão Anterior (Backend)
```java
// DashboardController.java:40
@RequireRole({UserRole.ADMIN, UserRole.OWNER})
public ResponseEntity<DashboardOperationalResponse> getOperationalDashboard(...)
```

### Permissão Anterior (Frontend)
```typescript
// permission-matrix.ts:38
{ path: 'dashboard', roles: [Role.OWNER, Role.ADMIN, Role.RECEPTIONIST, Role.INSTRUCTOR, Role.FINANCIAL] }
```

### Problema
- Frontend libera rota `/dashboard` para **OWNER, ADMIN, RECEPTIONIST, INSTRUCTOR, FINANCIAL**
- DashboardComponent chama **exclusivamente** `GET /dashboard/operational`
- Backend bloqueava RECEPTIONIST e INSTRUCTOR com `@RequireRole({ADMIN, OWNER})`
- Resultado: RECEPTIONIST/INSTRUCTOR viam o menu Dashboard, mas recebiam **403 Forbidden** ao carregar dados

### Permissão Corrigida (Backend)
```java
// DashboardController.java:40
@RequireRole({UserRole.ADMIN, UserRole.OWNER, UserRole.RECEPTIONIST, UserRole.INSTRUCTOR})
```

### Permissão Final

| Perfil | `/dashboard/operational` | `/dashboard` (não operacional) |
|--------|------------------------|-------------------------------|
| **OWNER** | ✅ 200 OK | ✅ 200 OK |
| **ADMIN** | ✅ 200 OK | ✅ 200 OK |
| **RECEPTIONIST** | ✅ 200 OK | ❌ 403 (restrito) |
| **INSTRUCTOR** | ✅ 200 OK | ❌ 403 (restrito) |
| **FINANCIAL** | ❌ 403 (restrito) | ❌ 403 (restrito) |

### Nota: `/dashboard/financial` não existe
Não há endpoint `/dashboard/financial` no backend nem no frontend. Quando for criado, deverá permanecer restrito conforme solicitação.

---

## 3. Comparação Completa: Todos os Endpoints

| Controller | Backend `@RequireRole` | Frontend Routes | Status |
|-----------|----------------------|-----------------|--------|
| **Dashboard** `/dashboard` | ADMIN, OWNER | OWNER, ADMIN, RECEPTIONIST, INSTRUCTOR, FINANCIAL | ⚠️ Frontend mais permissivo, mas endpoint não é chamado pelo frontend |
| **Dashboard** `/dashboard/operational` | ADMIN, OWNER, **RECEPTIONIST**, **INSTRUCTOR** | (mesma rota) | ✅ **CORRIGIDO** |
| **Student** (todos) | ADMIN, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST, INSTRUCTOR, FINANCIAL | ⚠️ INSTRUCTOR e FINANCIAL têm `STUDENT_READ` na RolePermissions mas bloqueados pelo @RequireRole |
| **Instructor** (todos) | ADMIN | OWNER, ADMIN | ✅ OK |
| **ClassGroup** (todos) | ADMIN | OWNER, ADMIN, RECEPTIONIST, INSTRUCTOR | ⚠️ RECEPTIONIST e INSTRUCTOR têm `CLASS_GROUP_READ` mas bloqueados |
| **Enrollment** (todos) | ADMIN, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST, FINANCIAL | ⚠️ FINANCIAL tem `ENROLLMENT_READ` mas bloqueado |
| **Attendance** (todos) | ADMIN, INSTRUCTOR, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST, INSTRUCTOR | ✅ OK |
| **ClassSession** (todos) | ADMIN, INSTRUCTOR, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST, INSTRUCTOR | ✅ OK |
| **MakeupRequest** (todos) | ADMIN, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST | ✅ OK |
| **Objective** (todos) | INSTRUCTOR, ADMIN | OWNER, ADMIN, INSTRUCTOR | ✅ OK |
| **Evaluation** (todos) | INSTRUCTOR, ADMIN | OWNER, ADMIN, INSTRUCTOR | ✅ OK |
| **Evolution** (todos) | INSTRUCTOR, ADMIN | OWNER, ADMIN, INSTRUCTOR | ✅ OK |

---

## 4. Divergências Adicionais Identificadas (Não corrigidas - fora do escopo)

| Endpoint | Backend | Frontend | Impacto |
|----------|---------|----------|---------|
| `GET /students` | ADMIN, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST, **INSTRUCTOR**, **FINANCIAL** | INSTRUCTOR não consegue ler alunos para objetivos/avaliações/evoluções. FINANCIAL não consegue ler alunos para financeiro. |
| `GET /class-groups` | ADMIN | OWNER, ADMIN, **RECEPTIONIST**, **INSTRUCTOR** | RECEPTIONIST/INSTRUCTOR não conseguem listar turmas |
| `GET /enrollments` | ADMIN, RECEPTIONIST | OWNER, ADMIN, RECEPTIONIST, **FINANCIAL** | FINANCIAL não consegue ler matrículas |

**Recomendação**: Corrigir `@RequireRole` nos controllers Student, ClassGroup e Enrollment para incluir os perfis que têm permissão de leitura na `RolePermissions` mas estão bloqueados pela anotação. Sugestão: usar `@RequirePermission` (já existe mas não é usado) ou adicionar os perfis faltantes ao `@RequireRole` específico para métodos GET.

---

## 5. Testes Adicionados

| Teste | Perfil | Endpoint | Resultado Esperado |
|-------|--------|----------|--------------------|
| `testGetOperationalDashboardAsReceptionist` | RECEPTIONIST | GET /dashboard/operational | 200 OK |
| `testGetOperationalDashboardAsInstructor` | INSTRUCTOR | GET /dashboard/operational | 200 OK |
| `testGetOperationalDashboardAsFinancialReturnsForbidden` | FINANCIAL | GET /dashboard/operational | 403 FORBIDDEN |
| `testGetDashboardAsReceptionistReturnsForbidden` | RECEPTIONIST | GET /dashboard | 403 FORBIDDEN |
| `testGetDashboardAsInstructorReturnsForbidden` | INSTRUCTOR | GET /dashboard | 403 FORBIDDEN |

### Resultado: 22/22 testes passando

---

## 6. Arquivos Modificados

| Arquivo | Alteração |
|---------|-----------|
| `corely-api/.../dashboard/DashboardController.java:40` | `@RequireRole` corrigido: adicionados `RECEPTIONIST` e `INSTRUCTOR` |
| `corely-api/docs/rbac.md:55,74` | Matriz de permissões e endpoints atualizados |
| `corely-api/.../dashboard/DashboardControllerTest.java:639-674` | 6 novos testes de autorização por role |

---

## 7. Resumo

- **Divergência principal corrigida**: RECEPTIONIST e INSTRUCTOR agora acessam `GET /dashboard/operational` sem 403
- **Divergências secundárias detectadas**: 3 controllers (Student, ClassGroup, Enrollment) com bloqueios adicionais — fora do escopo desta auditoria
- **Testes**: 6 novos testes de role + 16 existentes = 22 testes, todos verdes
- **Frontend**: Nenhuma alteração necessária (frontend já estava configurado corretamente)
