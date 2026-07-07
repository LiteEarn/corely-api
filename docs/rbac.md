# RBAC - Role Based Access Control

## Visão Geral

Sistema de autorização baseado em perfis (RBAC) implementado com Spring Security.
Utiliza `@RequireRole`, `@RequireAnyRole` e `@RequirePermission` para proteção de endpoints.

## Perfis (UserRole)

| Perfil | Descrição |
|--------|-----------|
| OWNER | Proprietário do sistema |
| ADMIN | Administrador do studio |
| RECEPTIONIST | Recepcionista |
| INSTRUCTOR | Instrutor |
| FINANCIAL | Financeiro |
| STUDENT | Aluno |

## Permissões

| Permissão | Descrição |
|-----------|-----------|
| DASHBOARD_VIEW | Visualizar dashboard |
| STUDENT_READ | Visualizar alunos |
| STUDENT_WRITE | Criar/editar alunos |
| INSTRUCTOR_READ | Visualizar instrutores |
| INSTRUCTOR_WRITE | Criar/editar instrutores |
| CLASS_GROUP_READ | Visualizar turmas |
| CLASS_GROUP_WRITE | Criar/editar turmas |
| ENROLLMENT_READ | Visualizar matrículas |
| ENROLLMENT_WRITE | Criar/editar matrículas |
| ATTENDANCE_READ | Visualizar presenças |
| ATTENDANCE_WRITE | Registrar presenças |
| SESSION_READ | Visualizar sessões |
| SESSION_WRITE | Criar/editar sessões |
| OBJECTIVE_READ | Visualizar objetivos |
| OBJECTIVE_WRITE | Criar/editar objetivos |
| EVALUATION_READ | Visualizar avaliações |
| EVALUATION_WRITE | Criar/editar avaliações |
| EVOLUTION_READ | Visualizar evoluções |
| EVOLUTION_WRITE | Criar/editar evoluções |
| MAKEUP_REQUEST_READ | Visualizar reposições |
| MAKEUP_REQUEST_WRITE | Criar/editar reposições |
| FINANCIAL_READ | Visualizar financeiro |
| FINANCIAL_WRITE | Criar/editar financeiro |
| USER_READ | Visualizar usuários |
| USER_WRITE | Criar/editar usuários |
| STUDIO_READ | Visualizar studio |
| STUDIO_WRITE | Criar/editar studio |

## Matriz de Permissões

| Recurso | OWNER | ADMIN | RECEPTIONIST | INSTRUCTOR | FINANCIAL | STUDENT |
|---------|-------|-------|--------------|------------|-----------|---------|
| Dashboard | READ | READ | READ | READ | - | - |
| Alunos | READ/WRITE | READ/WRITE | READ/WRITE | READ | READ | - |
| Instrutores | READ/WRITE | READ/WRITE | - | - | - | - |
| Turmas | READ/WRITE | READ/WRITE | READ | READ | - | - |
| Matrículas | READ/WRITE | READ/WRITE | READ/WRITE | - | READ | - |
| Presenças | READ/WRITE | READ/WRITE | READ/WRITE | READ/WRITE | - | - |
| Sessões | READ/WRITE | READ/WRITE | READ/WRITE | READ/WRITE | - | - |
| Reposições | READ/WRITE | READ/WRITE | READ/WRITE | - | - | - |
| Objetivos | READ/WRITE | READ/WRITE | - | READ/WRITE | - | READ |
| Avaliações | READ/WRITE | READ/WRITE | - | READ/WRITE | - | READ |
| Evoluções | READ/WRITE | READ/WRITE | - | READ/WRITE | - | READ |
| Financeiro | READ/WRITE | READ/WRITE | - | - | READ/WRITE | - |
| Usuários | READ/WRITE | READ/WRITE | - | - | - | - |
| Studio | READ/WRITE | READ/WRITE | - | - | - | - |

## Endpoints Protegidos

### Dashboard (`/dashboard`)
- `GET /dashboard` - ADMIN, OWNER
- `GET /dashboard/operational` - ADMIN, OWNER, RECEPTIONIST, INSTRUCTOR

### Alunos (`/students`)
- Todos os endpoints - ADMIN, RECEPTIONIST

### Instrutores (`/instructors`)
- Todos os endpoints - ADMIN

### Turmas (`/class-groups`)
- Todos os endpoints - ADMIN

### Matrículas (`/enrollments`)
- Todos os endpoints - ADMIN, RECEPTIONIST

### Presenças (`/attendance`, `/class-sessions/{id}/attendance`)
- Todos os endpoints - ADMIN, INSTRUCTOR, RECEPTIONIST

### Sessões (`/class-sessions`)
- Todos os endpoints - ADMIN, INSTRUCTOR, RECEPTIONIST

### Reposições (`/makeup-requests`)
- Todos os endpoints - ADMIN, RECEPTIONIST

### Objetivos (`/objectives`)
- Todos os endpoints - INSTRUCTOR, ADMIN

### Avaliações (`/evaluations`)
- Todos os endpoints - INSTRUCTOR, ADMIN

### Evoluções (`/evolutions`)
- Todos os endpoints - INSTRUCTOR, ADMIN

### Seed (`/dev/seed`)
- Todos os endpoints - ADMIN

## Anotações

### @RequireRole
Exige que o usuário tenha um dos perfis especificados.

```java
@RequireRole(UserRole.ADMIN)
@RequireRole({UserRole.ADMIN, UserRole.OWNER})
```

### @RequireAnyRole
Exige que o usuário tenha pelo menos um dos perfis especificados.

```java
@RequireAnyRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
```

### @RequirePermission
Exige que o usuário tenha todas as permissões especificadas.

```java
@RequirePermission({Permission.STUDENT_READ, Permission.STUDENT_WRITE})
```

## JWT
O token JWT inclui os claims:
- `role` - Perfil do usuário
- `permissions` - Lista de permissões

## Frontend
O frontend utiliza:
- `PermissionService` - Serviço para verificar permissões
- `RoleGuard` - Guard de rota para verificar perfil
- `HasPermissionDirective` - Diretiva estrutural `*hasPermission`
- `SessionService` - Gerenciamento de sessão com signals
