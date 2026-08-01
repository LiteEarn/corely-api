---
name: testing
description: Conhecimento reutilizavel de testes no Corely (JUnit, Mockito, Spring Boot Test, Testcontainers). Use ao criar ou atualizar testes unitarios e de integracao.
---

# Testing

Conhecimento reutilizavel sobre testes no Corely.

## Tipos de teste

- **Unitario**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`). Mocks de repositorios e dependencias. Testam o Service isoladamente.
- **Integracao**: `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`. Validam repositorios, services, controllers e o banco real.
- **E2E/Containers**: Testcontainers para validar comportamento com PostgreSQL real quando necessario.

## Convencoes

- Um teste por classe de servico (`XxxServiceTest`).
- Nomear testes descrevendo o comportamento: `create_shouldCreateSnapshotAndPersist`, `findById_shouldThrowException_whenNotFound`.
- Usar AssertJ (`assertThat`) para assercoes expressivas.
- `when(...).thenReturn(...)` para stubbing; `verify(...)` quando o comportamento exige confirmacao.
- Cenarios importantes: sucesso, validacao, regra de negocio violada, entidade inexistente, edge cases (null, vazio).

## Boas praticas

- Nao testar implementacao (mocks internos), testar comportamento observavel.
- Em testes unitarios, isolar totalmente o Service (mock de repositorios).
- Em testes de integracao, validar tambem o isolamento multi-tenant.
- Executar apenas quando solicitado pela tarefa (convencao do AI_CONTEXT.md).
