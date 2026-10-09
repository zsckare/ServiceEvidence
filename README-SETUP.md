# ServiceEvidence — Sprint 1 setup

Copy the supplied `backend/src/main/...` files into the existing `backend` project, preserving their paths. Copy `infrastructure/docker-compose.yml` into the repository's `infrastructure` directory.

## Start database

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

## Start API

```bash
cd backend
./gradlew bootRun
```

## Verify

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/v1/info
```

The first endpoint should respond `200` with health `UP` once PostgreSQL is ready; the protected endpoint should respond `403` or `401` (unauthenticated) until authentication is implemented. The Flyway migration creates the `tenants` table.

**Security:** The default database password is for local development only. Change it for shared or production environments. Do not expose the database port publicly.

## Note

This project uses the generated Groovy `build.gradle` and retains its existing Spring Boot and Kotlin versions. No ZIP or Gradle changes are needed for this sprint.
