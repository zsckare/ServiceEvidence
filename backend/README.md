# ServiceEvidence — Backend

Multi-tenant field-service SaaS API built with Kotlin, Spring Boot, PostgreSQL, Flyway, and JWT.

## Modules

- Identity, tenants and tenant-scoped roles (existing Sprint 1–2)
- Customers and locations (existing Sprint 3)
- Work orders, technician assignments, status transitions, audit events (Sprint 4)
- Evidence metadata registry (Sprint 5; **binary upload / object storage not implemented**)
- Tenant-scoped operational work-order summary (Sprint 6)

## Running

Configure PostgreSQL and `security.jwt.secret` through the existing application configuration, then run:

```bash
./gradlew clean build
./gradlew bootRun
```

Migrations V1–V5 run in order via Flyway. Do not alter already-applied migrations.

## Main endpoints

- `POST /api/v1/work-orders` — create (TENANT_ADMIN, SUPERVISOR)
- `GET /api/v1/work-orders` — list (TENANT_ADMIN, SUPERVISOR)
- `GET /api/v1/work-orders/mine` — assigned orders
- `GET /api/v1/work-orders/{id}` — detail (assigned technician or management)
- `PUT /api/v1/work-orders/{id}` — update (management)
- `POST /api/v1/work-orders/{id}/status` — transition
- `GET/POST /api/v1/work-orders/{id}/assignments` — list/create assignments
- `DELETE /api/v1/work-orders/{id}/assignments/{technicianId}` — unassign
- `GET /api/v1/work-orders/{id}/events` — audit history
- `GET/POST /api/v1/work-orders/{id}/evidence` — evidence metadata
- `GET /api/v1/reports/work-orders/summary` — management summary

All endpoints require a tenant-scoped JWT. Technician access to order detail and evidence requires an assignment. Technician can transition only from SCHEDULED to IN_PROGRESS and from IN_PROGRESS to COMPLETED.

## Scope and limitations

Evidence registration accepts an object key, MIME type, byte length, and SHA-256 **but does not upload, store, validate or serve the binary**. Do not expose this metadata endpoint to untrusted clients without first implementing trusted object-storage uploads, server-side object ownership and hash verification. Digital signatures, PDF report generation, notifications, recurring scheduling, and automated integration tests are **not included**. The project is a functional backend increment, not a claim of a production-complete SaaS.
