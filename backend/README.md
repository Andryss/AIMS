# AIMS Backend

Spring Boot 3.5 / Java 17 REST API for the AIMS incident management system.

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
