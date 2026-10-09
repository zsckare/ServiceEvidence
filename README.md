# ServiceEvidence

Multi-tenant B2B SaaS for work orders, field-service evidence and customer approvals. **Sprint 2 backend:** global platform administrator, tenant registry, users, JWT access tokens and rotating refresh sessions.

## Stack
Kotlin 2.3.21, Spring Boot 4.1.1, Java 21, Gradle, PostgreSQL, Flyway, Spring Security, Docker Compose.

## Local setup

1. Copy `.env.example` to `.env` at the repository root: `cp .env.example .env`.
2. Set `DB_PASSWORD`, `BOOTSTRAP_ADMIN_EMAIL`, and `BOOTSTRAP_ADMIN_PASSWORD` in `.env`. Generate a JWT signing secret with `openssl rand -hex 32` and paste its output as `JWT_SECRET` (do not use `$(...)` inside `.env`).
3. Start PostgreSQL from the repository root: `docker compose --env-file .env -f infrastructure/docker-compose.yml up -d`.
4. Run the backend from `backend/`: `./gradlew bootRun`. Spring Boot imports `../.env` as a properties file during local development.
5. After initial SUPER_ADMIN creation, remove `BOOTSTRAP_ADMIN_PASSWORD` from `.env`; restart the backend.

**Security:** `.env` is ignored by Git. Keep `JWT_SECRET` stable between restarts. Use a real secret manager and HTTPS in production. If PostgreSQL's volume was initialized with an earlier password, changing `DB_PASSWORD` in `.env` does not change the existing database password automatically.


## Authentication flow

1. `POST /api/v1/auth/login` with `{"email":"admin@example.com","password":"..."}` → `accessToken`, `refreshToken`.
2. Use `Authorization: Bearer <accessToken>` on protected endpoints.
3. `POST /api/v1/platform/tenants` (global SUPER_ADMIN only) with:
   ```json
   {"code":"ACME","name":"Acme Services","adminEmail":"owner@acme.example","adminName":"Acme Owner","adminPassword":"a-long-unique-password"}
   ```
4. `POST /api/v1/auth/tenant-context` with `{"tenantId":"<tenant UUID>"}` and a valid access token → a **tenant-scoped** access token.
5. `GET /api/v1/users` and `POST /api/v1/users` require a tenant-scoped token and either a global administrator or the tenant administrator. POST body: `{"email":"worker@acme.example","fullName":"Worker","password":"a-long-unique-password","role":"TECHNICIAN"}`.
6. `POST /api/v1/auth/refresh` with `{"refreshToken":"..."}` rotates the refresh token and returns a **global** access token; select a tenant again.
7. `POST /api/v1/auth/logout` with the refresh token revokes that refresh session.

### Routes

| Method | Route | Access |
|---|---|---|
| GET | `/actuator/health` | Public |
| POST | `/api/v1/auth/login`, `/api/v1/auth/refresh` | Public |
| GET | `/api/v1/auth/me`, `/api/v1/auth/available-tenants` | Authenticated |
| POST | `/api/v1/auth/logout`, `/api/v1/auth/tenant-context` | Authenticated |
| GET, POST | `/api/v1/platform/tenants` | Global SUPER_ADMIN |
| PATCH | `/api/v1/platform/tenants/{id}/status` | Global SUPER_ADMIN |
| GET, POST | `/api/v1/users` | Tenant context + tenant admin or SUPER_ADMIN |

## Security considerations / pending hardening

- Tenant context is signed into JWTs, never trusted from an arbitrary request header. Tenant-scoped operations must always filter database queries by the tenant claim.
- Refresh tokens are random, hashed at rest and rotated. Logout revokes the supplied refresh token, **not already issued access JWTs** (valid up to 15 minutes).
- Existing user email cannot be enrolled into another tenant through the basic create endpoint; a verified invitation flow is planned.
- Add rate limiting, account lockout policy, password recovery, structured audit metadata, secret rotation, and integration tests before public production launch.
- All API data-access modules introduced later must enforce tenant isolation. Platform admins must select an active tenant before operating on tenant data.

## Verification

`cd backend && ./gradlew test` (requires Gradle distribution and dependencies). This package has **not been compiled in this environment** because the Gradle distribution could not be downloaded.
