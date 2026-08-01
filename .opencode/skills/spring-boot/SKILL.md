---
name: spring-boot
description: Conhecimento reutilizavel de Spring Boot para o Corely. Use quando implementar controllers, services, repositories, configuracoes, schedulers, exceptions e beans no backend Java 21.
---

# Spring Boot

Conhecimento reutilizavel sobre Spring Boot no contexto do Corely (Java 21).

## Estrutura de camadas

- **Controller**: expoe endpoints REST. Nunca contem regra de negocio. Usa DTOs.
- **Service**: contem regra de negocio. `@Service`, `@Transactional`.
- **Repository**: acesso a dados via Spring Data JPA.
- **Mapper**: converte Entity <-> DTO.

## Boas praticas

- Use `@RequiredArgsConstructor` (Lombok) para injecao por construtor; prefira construtor a `@Autowired` em campo.
- `@Transactional(readOnly = true)` em metodos de leitura.
- Schedulers com `@Scheduled`; isolar em classes dedicadas (nunca no Controller).
- Exceptions centralizadas: `BusinessException` e `ResourceNotFoundException` em `br.com.corely.shared.exception`, tratadas por um `@RestControllerAdvice`.
- Bean Validation (`@Valid`) nos DTOs de request.
- Prefira Optional no retorno de repositorios quando a ausencia e possivel.

## Configuracao

- Configuracoes por ambiente: `application.yaml`, `application-dev.yaml`, `application-test.yaml`.
- Perfil ativo via `spring.profiles.active`.
- Nunca expor segredos em configuracao versionada; use variaveis de ambiente.

## Convencoes Corely

- Multi-tenant: respeitar `studioId` em toda consulta (ver skill `multi-tenant`).
- Persistencia: usar `@EntityGraph` e batch queries (ver skill `persistence`).
- OpenAPI: anotar endpoints e DTOs com Swagger (`@Operation`, `@Tag`, `@Schema`).
