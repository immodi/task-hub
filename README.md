# TaskHub

A task and project management REST API — projects with members, tasks with tags and a status
workflow, comments, and an asynchronous activity log.

![CI](https://github.com/immodi/task-hub/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-27-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.1-6BA539)
![Tests](https://img.shields.io/badge/tests-85%20passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

Built with **Spring Boot 4** and **Java 27**: Spring Data JPA, Spring Security with self-issued
HS256 JWTs, an in-memory H2 database, springdoc-openapi, Actuator, and JUnit 5. It was written as a
hands-on bridge from .NET / ASP.NET Core, so the code and this README point out the equivalents
along the way.

## Quick start

Requirements: **JDK 27** and **Maven 3.9+** (or use [Docker](#docker)).

```bash
mvn spring-boot:run
```

Then open <http://localhost:9090> — it redirects to the Swagger UI, where you can try every endpoint.

1. `POST /api/auth/login` with `{"username": "alice", "password": "Password1"}`.
2. Copy the `token` from the response.
3. Click **Authorize** and paste the token.

Seeded users, all with password `Password1`: `admin` (ADMIN), `alice` and `bob` (USER).

```bash
PORT=8081 mvn spring-boot:run          # different port
mvn test                               # run the test suite
```

If your toolchain complains about Java 27 class-file versions, set `<java.version>` in `pom.xml`
to `25` (the current LTS).

### Docker

```bash
docker compose up --build
# or
docker build -t taskhub . && docker run -p 9090:9090 taskhub
```

## API

The full, generated spec is checked in at [`docs/openapi.json`](docs/openapi.json), served live at
`/v3/api-docs`, and browsable at `/swagger-ui.html`.

| Group | Method & path | Notes |
|---|---|---|
| Auth | `POST /api/auth/register` | Public. Creates a USER and returns a token. |
| | `POST /api/auth/login` | Public. Exchanges credentials for a JWT. |
| | `GET /api/auth/me` | The authenticated user. |
| Projects | `GET /api/projects` | Visible projects only; paged and sorted. |
| | `POST /api/projects` | Caller becomes owner. |
| | `GET`, `PUT`, `DELETE /api/projects/{id}` | `PUT`/`DELETE` are owner-only. |
| | `POST /api/projects/{id}/archive` · `/unarchive` | Owner-only. Archived projects reject new tasks. |
| | `PUT`, `DELETE /api/projects/{id}/members/{userId}` | Owner-only. Removing a member unassigns their tasks. |
| | `GET /api/projects/{id}/stats` | Total / overdue / per-status, cached. |
| Tasks | `POST /api/projects/{projectId}/tasks` | Caller becomes reporter. |
| | `GET /api/projects/{projectId}/tasks` | Filter by `status`, `priority`, `assigneeId`, `tag`, `q`, `overdue`. |
| | `GET`, `PUT`, `DELETE /api/tasks/{id}` | `PUT` takes an optional `version`. |
| | `PATCH /api/tasks/{id}/status` | Enforces the workflow; illegal moves return 422. |
| | `PUT /api/tasks/{id}/assignee` | `null` unassigns. |
| Comments | `POST`, `GET /api/tasks/{taskId}/comments` | |
| | `DELETE /api/comments/{id}` | Author, project owner or admin. |
| Admin | `GET /api/admin/users` · `PATCH /api/admin/users/{id}/role` | ADMIN only. |
| | `GET /api/admin/activity` | ADMIN only. Optional `?projectId=` filter. |
| Ops | `GET /actuator/health` · `/info` · `/metrics` · `/caches` | |

`requests.http` has ready-made requests for the IntelliJ HTTP client and the VS Code REST Client.

## Features

- **Domain** — users, projects with members, tasks with tags, comments and an activity log.
- **Auth** — self-issued HS256 JWTs, role claims, URL rules plus method-level `@PreAuthorize`.
- **Search** — dynamic filters (status, priority, assignee, tag, free text, overdue) with paging
  and sorting. Sort fields are validated, so a typo returns a clear `400` instead of a `500`.
- **Workflow** — status changes follow a state machine; invalid transitions return `422`.
- **Concurrency** — updates accept a `version`; a stale one returns `409`.
- **Errors** — RFC 9457 `application/problem+json`, with a per-field validation message map.
- **Events** — a sealed event hierarchy handled by an exhaustive `switch`.
- **Background work** — an async activity log written after commit, plus a scheduled scan that
  publishes an overdue-task gauge at `/actuator/metrics/taskhub.overdue.tasks`.
- **Caching** — project stats are cached and evicted after task changes commit.
- **Ops** — request-id logging (MDC + `X-Request-Id`), virtual threads, Actuator, OpenAPI 3.1.

## How it works

A few decisions worth calling out, because they are the interesting parts:

- **Authorization is layered.** URL rules in `SecurityConfig` cover the coarse cases (public auth
  endpoints, `/api/admin/**`), `@PreAuthorize` guards admin methods, and `AccessService` does the
  resource-based checks ("are you a member of this project / the owner of this task?").
- **DTOs are mapped inside transactions.** `open-in-view` is off, so lazy associations are resolved
  in the service layer via `Mappers`, and `default_batch_fetch_size` keeps the N+1s down.
- **Domain events decouple the audit trail.** Services publish `TaskEvent`s; `ActivityLogListener`
  handles them after commit, on a virtual thread, in its own transaction. Events carry ids, never
  entities, because the publishing transaction is already closed.
- **Caching is proxy-based.** `StatsService` lives in its own bean so `@Cacheable` actually applies
  (a self-invocation would bypass the proxy), and an `@TransactionalEventListener(AFTER_COMMIT)`
  evicts the cache only once the change is durable.

## Testing

```bash
mvn test
```

85 tests, three flavours:

| Kind | Location | Covers |
|---|---|---|
| Unit | `src/test/.../unit` | status transitions, password validator, sort validation, DTO mapping |
| Service integration | `TaskServiceTest` | services against a real context, rolled back per test |
| API over HTTP | `src/test/.../api` | auth, validation, permissions, filtering, paging, concurrency, cascading deletes, async activity log, cache eviction, problem+json, the OpenAPI document |

The API tests boot the real application on a random port and call it over HTTP — the .NET
equivalent of `WebApplicationFactory` + `HttpClient`.

CI runs `mvn verify` on every push and pull request (see `.github/workflows/ci.yml`).

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `PORT` | `9090` | HTTP port. |
| `TASKHUB_JWT_SECRET` | a dev-only placeholder | HS256 signing key; use 32+ bytes. |
| `taskhub.security.token-ttl` | `PT2H` | Token lifetime. |
| `taskhub.overdue-scan-interval` | `PT1M` | How often the overdue scan refreshes the `taskhub.overdue.tasks` gauge. |

## Project layout

```
src/main/java/com/example/taskhub
├── config/       feature switches, typed properties, request-id filter, OpenAPI
├── security/     JWT issue/verify, Spring Security config, AuthUser argument resolver
├── domain/       JPA entities and enums
├── repository/   Spring Data repositories
├── service/      business logic, authorization, specifications, sorting, scheduler
├── event/        domain events and the async listener
├── dto/          request/response records and the mappers
├── web/          controllers and the global exception handler
├── seed/         demo data
└── exception/    the single ApiException type
```

Regenerate the checked-in spec while the app is running:

```bash
curl -s http://localhost:9090/v3/api-docs | jq . > docs/openapi.json
```

## Ideas / next steps

1. Enable `org.hibernate.SQL: DEBUG` and watch the SQL for the task search. Then remove
   `default_batch_fetch_size` to see the N+1 queries appear.
2. Call `StatsService.compute` from inside the same bean and watch the cache stop working
   (proxy self-invocation).
3. Add a new `TaskEvent` record and watch the compiler flag the unhandled case in the listener.
4. Replace the manual `version` check with `If-Match` / `ETag`.
5. Swap H2 for PostgreSQL using Testcontainers.
6. Add refresh tokens and rate limiting on the login endpoint.

## Known limitations

- Roles are baked into the JWT, so an admin role change applies after the current token expires.
- There are no refresh tokens and no rate limiting.
- The data lives in memory and is lost on restart.

## License

[MIT](LICENSE).
