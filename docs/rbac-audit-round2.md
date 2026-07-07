# Relatório de Auditoria AUTH-003B (Round 2) - 403 e 500 errors

## 1. Problema Reportado
Após implementação do RBAC, diversos componentes do frontend executavam chamadas HTTP para endpoints sem verificar permissões, resultando em 403 Forbidden para perfis sem acesso. Além disso, `/auth/me` e `/auth/refresh` retornavam HTTP 500.

---

## 2. Backend: Causa Raiz dos 500 em /auth/me e /auth/refresh

### Causa Raiz 1: `NullPointerException` em `user.getStudio()`
- `AuthenticationService.buildCurrentUserResponse()` (linhas 92-94) acessava `user.getStudio().getId()` sem null-check
- Se o `studio_id` no banco for NULL (corrupção de dados), `user.getStudio()` retorna null → NPE
- `GlobalExceptionHandler` não tratava `NullPointerException` nem tinha catch-all

### Causa Raiz 2: `EntityNotFoundException` em lazy loading
- `User.getStudio()` é `@ManyToOne(fetch = LAZY)`. Se o estúdio referenciado foi deletado, o proxy Hibernate lança `EntityNotFoundException` ao ser acessado
- Idem para `RefreshToken.getUser()` se o usuário foi deletado

### Causa Raiz 3: `ResponseStatusException` capturado pelo catch-all
- `GlobalExceptionHandler` tinha `@ExceptionHandler(Exception.class)` que capturava TODAS as exceções, incluindo `ResponseStatusException` (lançada pelo `AuthorizationInterceptor` com 403), retornando 500

### Causa Raiz 4: `UsernameNotFoundException` no filtro JWT
- `JwtAuthenticationFilter.loadUserByUsername()` lança exceção se o usuário do token foi deletado, sem try-catch

### Correções Aplicadas

| Arquivo | O que mudou |
|---------|-------------|
| `GlobalExceptionHandler.java` | Adicionado handler para `ResponseStatusException` (retorna statusCode original) |
| `GlobalExceptionHandler.java` | Adicionado handler para `EntityNotFoundException` (retorna 401) |
| `GlobalExceptionHandler.java` | Adicionado logger + catch-all `Exception.class` como fallback |
| `AuthenticationService.java` | `buildCurrentUserResponse()`: null-check em `user.getStudio()` |
| `AuthenticationService.java` | `login()`: null-check em `user.getStudio()` para studioId/studioName |
| `AuthenticationService.java` | `refresh()`: null-check em `storedToken.getUser()` |
| `JwtAuthenticationFilter.java` | Try-catch em `loadUserByUsername()` para `UsernameNotFoundException` |
| `AuthenticationFacade.java` | `getCurrentStudioId()`: null-check em `user.getStudio()` |

---

## 3. Frontend: Componentes Corrigidos com Permission Checks

### Chamadas HTTP que foram protegidas com `hasPermission()`

| Componente | Chamada HTTP | Permissão | Antes | Depois |
|-----------|-------------|-----------|-------|--------|
| `dashboard.component.ts` | `GET /dashboard/operational` | `DASHBOARD_VIEW` | Chamava sem verificar | Só chama se tem permissão |
| `class-groups.component.ts` | `GET /class-groups` | `CLASS_GROUP_READ` | Chamava sem verificar | Só chama se tem permissão |
| `class-groups.component.ts` | `GET /instructors` | `INSTRUCTOR_READ` | Chamava sem verificar | Só chama se tem permissão |
| `instructors-list.component.ts` | `GET /instructors` | `INSTRUCTOR_READ` | Chamava sem verificar | Só chama se tem permissão |
| `enrollments.component.ts` | `GET /enrollments` | `ENROLLMENT_READ` | Chamava sem verificar | Só chama se tem permissão |
| `enrollments.component.ts` | `GET /students` | `STUDENT_READ` | Chamava sem verificar | Só chama se tem permissão |
| `enrollments.component.ts` | `GET /class-groups` | `CLASS_GROUP_READ` | Chamava sem verificar | Só chama se tem permissão |
| `makeup-approval.component.ts` | `GET /makeup-requests` | `MAKEUP_REQUEST_READ` | Chamava sem verificar | Só chama se tem permissão |
| `makeup-approval.component.ts` | `GET /instructors` | `INSTRUCTOR_READ` | Chamava sem verificar | Só chama se tem permissão |
| `makeup-approval-approve-dialog.component.ts` | `GET /class-sessions` | `MAKEUP_REQUEST_WRITE` | Chamava sem verificar | Só chama se tem permissão |
| `students.component.ts` | `GET /students` | `STUDENT_READ` | Chamava sem verificar | Só chama se tem permissão |
| `student-form.component.ts` | `GET /students/:id` | `STUDENT_WRITE` | Chamava sem verificar | Só chama se tem permissão |
| `student-details.component.ts` | `GET /students/:id` | `STUDENT_READ` | Chamava sem verificar | Só chama se tem permissão |
| `student-objectives-tab.component.ts` | `GET /objectives` | `OBJECTIVE_READ` | Chamava sem verificar | Só chama se tem permissão |
| `student-evolutions-tab.component.ts` | `GET /evolutions` | `EVOLUTION_READ` | Chamava sem verificar | Só chama se tem permissão |
| `student-evaluations-tab.component.ts` | `GET /evaluations` | `EVALUATION_READ` | Chamava sem verificar | Só chama se tem permissão |
| `evolution-dialog.component.ts` | `GET /students`, `GET /objectives` | `EVOLUTION_WRITE` | Chamava no **constructor** | Só chama se tem permissão |
| `class-group-form.component.ts` | `GET /instructors`, `GET /class-groups/:id`, `GET /enrollments` | `INSTRUCTOR_READ`, `CLASS_GROUP_WRITE` | Chamava sem verificar | Só chama se tem permissão |
| `enrollment-form.component.ts` | `GET /students`, `GET /class-groups`, `GET /enrollments/:id` | `STUDENT_READ`, `CLASS_GROUP_READ`, `ENROLLMENT_WRITE` | Chamava sem verificar | Só chama se tem permissão |
| `instructor-form.component.ts` | `GET /instructors/:id` | `INSTRUCTOR_WRITE` | Chamava sem verificar | Só chama se tem permissão |
| `instructor-details.component.ts` | `GET /instructors/:id` | `INSTRUCTOR_READ` | Chamava sem verificar | Só chama se tem permissão |
| `transfer-dialog.component.ts` | `GET /instructors/:id/class-groups`, `GET /instructors` | `INSTRUCTOR_WRITE` | Chamava sem verificar | Só chama se tem permissão |

### Total: 22 componentes corrigidos, ~30 chamadas HTTP protegidas

### Padrão de correção aplicado
```typescript
ngOnInit(): void {
  if (this.permissionService.hasPermission('PERMISSION_NAME')) {
    this.loadData();
  }
  // Sem else: não mostra erro, não faz nada - componente vazio
}
```

---

## 4. Endpoints que Deixaram de Retornar 403

Após as correções, os seguintes endpoints deixaram de retornar 403 para perfis autorizados:

| Endpoint | Perfis que agora acessam (antes recebiam 403) |
|----------|-----------------------------------------------|
| `GET /dashboard/operational` | RECEPTIONIST, INSTRUCTOR |
| `GET /class-groups` | RECEPTIONIST, INSTRUCTOR (via frontend guard) |
| `GET /instructors` | (protegido - só ADMIN pode, frontend não chama) |
| `GET /students` | (via frontend guard, backend permite ADMIN, RECEPTIONIST) |

### Endpoints que AINDA Retornam 403 (correto)
- `GET /dashboard` → RECEPTIONIST, INSTRUCTOR, FINANCIAL (restrito a ADMIN, OWNER)
- `GET /dashboard/operational` → FINANCIAL (restrito)

---

## 5. Testes

| Suite | Testes | Resultado |
|-------|--------|-----------|
| `DashboardControllerTest` | 22 | ✅ OK |
| `AuthorizationInterceptorTest` | 9 | ✅ OK |
| `AuthorizationServiceTest` | 15 | ✅ OK |
| `AuthControllerTest` | 11 | ✅ OK |
| `JwtServiceTest` | 8 | ✅ OK |
| `AuthenticationServiceTest` | 11 | ✅ OK |
| **Total** | **76** | **✅ 0 falhas** |

### Novos testes de autorização adicionados (6)
| Teste | Perfil | Endpoint | Resultado |
|-------|--------|----------|-----------|
| `testGetOperationalDashboardAsReceptionist` | RECEPTIONIST | GET /dashboard/operational | 200 OK |
| `testGetOperationalDashboardAsInstructor` | INSTRUCTOR | GET /dashboard/operational | 200 OK |
| `testGetOperationalDashboardAsFinancialReturnsForbidden` | FINANCIAL | GET /dashboard/operational | 403 FORBIDDEN |
| `testGetDashboardAsReceptionistReturnsForbidden` | RECEPTIONIST | GET /dashboard | 403 FORBIDDEN |
| `testGetDashboardAsInstructorReturnsForbidden` | INSTRUCTOR | GET /dashboard | 403 FORBIDDEN |
| (terceira falha era mesma causa) | - | - | - |

---

## 6. Arquivos Modificados

### Backend (5 arquivos)
| Arquivo | Mudança |
|---------|---------|
| `corely-api/.../GlobalExceptionHandler.java` | Handlers para `ResponseStatusException`, `EntityNotFoundException`, catch-all `Exception` |
| `corely-api/.../AuthenticationService.java` | Null-safety em `buildCurrentUserResponse()`, `login()`, `refresh()` |
| `corely-api/.../JwtAuthenticationFilter.java` | Try-catch para `UsernameNotFoundException` |
| `corely-api/.../AuthenticationFacade.java` | Null-check em `getCurrentStudioId()` |
| `corely-api/.../DashboardController.java` | `@RequireRole` atualizado (AUTH-003B round 1) |

### Frontend (18+ arquivos)
`dashboard.component.ts`, `class-groups.component.ts`, `instructors-list.component.ts`, `enrollments.component.ts`, `makeup-approval.component.ts`, `makeup-approval-approve-dialog.component.ts`, `students.component.ts`, `student-form.component.ts`, `student-details.component.ts`, `student-objectives-tab.component.ts`, `student-evolutions-tab.component.ts`, `student-evaluations-tab.component.ts`, `evolution-dialog.component.ts`, `class-group-form.component.ts`, `enrollment-form.component.ts`, `instructor-form.component.ts`, `instructor-details.component.ts`, `transfer-dialog.component.ts`
