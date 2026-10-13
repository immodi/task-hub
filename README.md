# TaskHub

A task and project management REST API built with Spring Boot, written as a practice project for developers coming from .NET. It has JWT auth, JPA with an in-memory H2 database, a Swagger UI, and a test suite.

## Quick start

Requirements: JDK 27 (or 25, see below) and Maven 3.9+.

```bash
mvn spring-boot:run
```

Then open http://localhost:9090/. It redirects to the Swagger UI, where you can try every endpoint.

1. Call `POST /api/auth/login` with `{"username": "alice", "password": "Password1"}`.
2. Copy the `token` from the response.
3. Click **Authorize** and paste the token.

Seeded users, all with password `Password1`: `admin` (ADMIN), `alice` and `bob` (USER).

Change the port with `PORT=8081 mvn spring-boot:run`. The OpenAPI spec is at `/v3/api-docs`, and `requests.http` has ready-made requests for IntelliJ or the VS Code REST Client.

If your JDK or a dependency complains about Java 27, set `<java.version>` in `pom.xml` to `25`.

## Tests

```bash
mvn test
```

| Kind | Location | Covers |
|---|---|---|
| Unit | `src/test/.../unit` | task status transitions, password validator |
| Service integration | `TaskServiceTest` | services against a real context, rolled back per test |
| API over HTTP | `src/test/.../api` | auth, validation, permissions, filtering, paging, optimistic locking, cascading deletes, async activity log, cache eviction |

## Features

- **Domain:** users, projects with members, tasks with tags, comments, activity log.
- **Auth:** self-issued HS256 JWTs, roles, URL rules plus `@PreAuthorize`.
- **Tasks:** filter by status, priority, assignee, tag, text and overdue, with paging and sorting.
- **Workflow:** status changes follow a state machine, and invalid moves return 422.
- **Concurrency:** updates accept a `version`, and a stale one returns 409.
- **Errors:** RFC 9457 problem+json, with per-field validation messages.
- **Events:** sealed event types and an exhaustive pattern-matching switch.
- **Background work:** an async activity log written after commit, and a scheduled overdue scan.
- **Caching:** project stats are cached and evicted after task changes commit.
- **Other:** request-id logging, virtual threads, Actuator, Swagger UI (springdoc 3.x).

## Project layout

```
src/main/java/com/example/taskhub
├── config/       app switches, typed properties, request-id filter, OpenAPI
├── security/     JWT, Spring Security config, AuthUser argument resolver
├── domain/       JPA entities and enums
├── repository/   Spring Data repositories
├── service/      business logic, authorization, specifications, scheduler
├── event/        domain events and listeners
├── dto/          request/response records and mappers
├── web/          controllers and the global exception handler
└── seed/         demo data
```

## .NET to Spring cheat sheet

| .NET | Spring |
|---|---|
| `Program.cs`, `AddX()` | `@SpringBootApplication`, `@Configuration`, `@Bean` |
| Constructor DI | Constructor injection, `@Service` / `@Component` |
| `[ApiController]`, `[HttpGet]` | `@RestController`, `@GetMapping` |
| `[FromBody]`, `[FromQuery]` | `@RequestBody`, `@RequestParam` / `@ModelAttribute` |
| DataAnnotations | Jakarta Bean Validation |
| EF Core `DbContext` | Spring Data repositories + JPA |
| `[Timestamp]` | `@Version` |
| `IOptions<T>` | `@ConfigurationProperties` |
| Middleware | `Filter` |
| ProblemDetails | `@RestControllerAdvice` + `ProblemDetail` |
| `BackgroundService` | `@Scheduled`, `@Async` |
| MediatR notifications | `ApplicationEventPublisher` + `@EventListener` |
| `IMemoryCache` | `@Cacheable` |
| xUnit + `WebApplicationFactory` | JUnit 5 + `@SpringBootTest` |

## Things to try

1. Enable `org.hibernate.SQL: DEBUG` and watch the SQL for the task search. Then remove `default_batch_fetch_size` to see N+1 queries appear.
2. Call `StatsService.compute` from inside the same bean and see the cache stop working (proxy self-invocation).
3. Add a new `TaskEvent` record and watch the compiler flag the unhandled case in the listener.
4. Call a list endpoint with `?sort=bogus`. It returns a 500 because sort fields aren't whitelisted. Fix it.
5. Replace the manual `version` check with `If-Match` / `ETag`.
6. Swap H2 for PostgreSQL using Testcontainers.

## Known limitations

- Roles are baked into the JWT, so a role change applies only after the token expires.
- No refresh tokens or rate limiting.
- The data is in memory and is lost on restart.
