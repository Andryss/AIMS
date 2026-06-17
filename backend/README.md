# AIMS Backend

Spring Boot 3.5 / Java 17 REST API for the AIMS incident management system.

See also: [project README](../README.md) · [frontend README](../frontend/README.md)

## Technology stack and licenses

### Runtime (shipped in the Spring Boot JAR)

| Component | Version | Role |
|-----------|---------|------|
| [Spring Boot](https://spring.io/projects/spring-boot) | 3.5.14 | Application framework (Web, Security, JPA, Validation, Actuator) |
| Java | 17 | Runtime |
| [PostgreSQL JDBC](https://jdbc.postgresql.org/) | 42.7.10 | Database driver (`runtime`) |
| [Liquibase](https://www.liquibase.com/) | 4.31.1 | Schema migrations |
| [JJWT](https://github.com/jwtk/jjwt) | 0.12.6 | JWT creation and parsing |
| [db-queue](https://github.com/yoomoney/db-queue) (`db-queue-core`, `db-queue-spring`) | 15.1.0 | Background task queue on PostgreSQL |
| [Lombok](https://projectlombok.org/) | 1.18.46 | Compile-time boilerplate reduction (not required at runtime) |
| [swagger-annotations](https://github.com/swagger-api/swagger-core) | 2.2.23 | OpenAPI annotation types for generated API |

Spring Boot brings transitive dependencies (Spring Framework, Hibernate, Jackson, Logback, etc.). Generated API interfaces and models live under `target/generated-sources/openapi/` (OpenAPI Generator).

**Infrastructure (Docker, not a Maven dependency):** PostgreSQL 15 (`postgres:15-alpine` in repo-root `docker-compose.yml`) — [PostgreSQL License](https://www.postgresql.org/about/licence/) (permissive).

### Build, code generation, and quality (not required in production runtime)

| Tool | Version | Role |
|------|---------|------|
| [OpenAPI Generator](https://openapi-generator.tech/) | 7.8.0 | Server stubs from `api.yaml` |
| [Checkstyle](https://checkstyle.org/) (Maven plugin) | 3.6.0 | Style checks at `validate` |
| [JaCoCo](https://www.jacoco.org/) | 0.8.11 | Test coverage (minimum 80% lines) |

### Test scope only

| Library | Version | Role |
|---------|---------|------|
| Spring Boot Test, Spring Security Test | BOM-managed | Integration / API tests |
| [Zonky embedded PostgreSQL](https://github.com/zonkyio/embedded-database-spring-test) | 2.6.0 / 2.1.0 | In-memory Postgres for tests |

### Licenses and commercial use

This section describes **project policy** for dependency licensing. It is not legal advice; confirm requirements with your organization if needed.

**Allowed in production / runtime artifacts** — commercial use OK without open-sourcing your application code:

- MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, 0BSD, Unlicense

**Allowed only as build or test tools** (not shipped in the deployable JAR):

- EPL-1.0 / EPL-2.0 (JaCoCo; Logback is dual-licensed EPL-2.0 / LGPL-2.1 as part of Spring Boot’s default logging stack)
- LGPL-2.1 (Checkstyle plugin; Logback’s LGPL option when used as a separate library)

**Not acceptable** as deliberate runtime dependencies: GPL, AGPL, or LGPL libraries that would be distributed as part of the product without a compliant linking exception.

#### Direct dependency license summary

| Dependency | Version | License | Commercial use |
|------------|---------|---------|----------------|
| Spring Boot starters | 3.5.14 | Apache-2.0 | Yes |
| PostgreSQL JDBC | 42.7.10 | BSD-2-Clause | Yes |
| Liquibase | 4.31.1 | Apache-2.0 | Yes |
| JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | 0.12.6 | Apache-2.0 | Yes |
| db-queue-core / db-queue-spring | 15.1.0 | MIT | Yes |
| Lombok | 1.18.46 | MIT | Yes (compile-time) |
| swagger-annotations | 2.2.23 | Apache-2.0 | Yes |

#### Re-running the license audit

Download license metadata for compile/runtime dependencies (excludes `test` scope):

```bash
cd backend
./mvnw org.codehaus.mojo:license-maven-plugin:download-licenses -Dlicense.excludedScopes=test
```

Review `target/generated-resources/licenses.xml`.

## Prerequisites

- Java 17+
- Docker (optional, for PostgreSQL via `docker-compose.yml` at repo root)

## Quick start

```bash
cd backend
./mvnw spring-boot:run
```

By default the `dev` profile is active (`spring.profiles.default=dev`). Dev settings (demo JWT secret, demo data seed) live in `src/main/resources/application-dev.properties`.

For non-dev environments set:

```bash
export JWT_SECRET='your-256-bit-or-longer-secret'
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/aims'
export SPRING_DATASOURCE_USERNAME='aims'
export SPRING_DATASOURCE_PASSWORD='aims'
export AIMS_STORAGE_BASE_PATH='/tmp/aims-files'
```

The application fails fast on startup if `JWT_SECRET` is missing outside `dev` and `test` profiles.

## Build and test

```bash
cd backend
./mvnw verify
```

This runs Checkstyle, unit/integration tests (embedded PostgreSQL + Liquibase), and JaCoCo coverage (minimum 80% line coverage).

## API contract

OpenAPI spec: `src/main/resources/api/api.yaml`.

Regenerate server stubs and models:

```bash
cd backend
./mvnw generate-sources
```

Generated code is under `target/generated-sources/openapi/`.

## Profiles

| Profile | Purpose |
|---------|---------|
| `dev` | Local development; default JWT secret and `aims.demo-data.enabled=true` |
| `test` | Integration tests (`application-test.properties`) |

## Security notes

- `@PreAuthorize` enforces permissions on incidents, users, aliens, files, notifications, and monitoring alerts.
- Actuator: `/actuator/health` is public; other actuator endpoints require authentication; `/actuator/prometheus` is denied by default in `SecurityConfig`.

## Project layout

- `controller/` — OpenAPI `*ApiImpl` adapters
- `services/` — business logic, status workflows, db-queue processors
- `entity/` / `repository/` — JPA persistence
- `config/` — security, db-queue, validation
- `src/test/java/gov/mib/aims/backend/support/ApiTestFixtures.java` — shared MockMvc helpers for API tests

## External monitoring integration (UC1)

Push endpoint for external systems and local testing:

```bash
curl -sS -X POST http://localhost:8080/api/v1/integration/monitoring/events \
  -H 'Content-Type: application/json' \
  -H 'X-Integration-Api-Key: dev-monitoring-api-key' \
  -d '{
    "externalEventId": "demo-event-001",
    "sourceSystem": "EXTERNAL_MONITORING_V1",
    "detectedAt": "2025-06-01T12:00:00Z",
    "location": "Nevada desert sector 7",
    "eventType": "UNIDENTIFIED_SIGHTING",
    "description": "Thermal anomaly detected by external sensors",
    "mediaUrls": ["https://example.com/evidence/photo1.jpg"]
  }'
```

Dev API key: `aims.integration.monitoring.api-key` in `application-dev.properties` (override via env in production).
