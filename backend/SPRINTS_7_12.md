# ServiceEvidence — Sprints 7–12 (integration snapshot)

This is an **incremental backend**, not a production-ready commercial release.

## Implemented

- Sprint 7: private local-volume evidence upload (`POST /api/v1/work-orders/{workOrderId}/files`, multipart `file`, optional `kind` and `notes`) and authorized download (`GET /api/v1/work-orders/{workOrderId}/files/{evidenceId}`). Allowed types: JPEG, PNG, WebP, PDF; max 20 MB. Tenant-scoped directories, generated filenames, SHA-256 and basic magic-byte validation. Existing metadata-only endpoints remain compatible.
- Sprint 8: signature **image capture** via `kind=SIGNATURE` with JPEG/PNG; no cryptographic signature, legal attestation, or signer identity proof.
- Sprint 9: basic PDF work-order report at `GET /api/v1/work-orders/{workOrderId}/report.pdf`, includes metadata and evidence references, not embedded images or signatures.
- Sprint 10: persistent in-app notification inbox (`GET /api/v1/notifications`, `POST /api/v1/notifications/{id}/read`); a notification is created when a technician is assigned. No email/SMS delivery.
- Sprint 11: per-tenant configuration (`GET/PUT /api/v1/settings`). No subscription plans, billing, or quotas.
- Sprint 12: Dockerfile, compose file, and versioned migration V6. No complete load tests, CI, vulnerability review or production hardening.

## Run

Local: `./gradlew clean build && ./gradlew bootRun` (PostgreSQL and env configured).
Docker: first build JAR locally with `./gradlew bootJar`; then set `DB_PASSWORD` and a strong `JWT_SECRET` in your environment and run `docker compose up --build`.

## Important limitations

- File writes and DB transactions are not fully atomic; a DB rollback after a successful file write can leave an orphan. Implement cleanup jobs and virus scanning before production.
- Uploads are served as attachments; only allowlisted MIME types are accepted with basic header validation. Strong content scanning, image decoding, encryption-at-rest and object storage integration are future work.
- PDF output is a minimal single-page summary with ASCII fallback; use a Unicode font and pagination for production.
- `POST /api/v1/work-orders/{id}/evidence` is still metadata-only and can register external keys; it does not upload bytes.
- `V6` is an additive migration; run in a disposable database first and back up before upgrading a real deployment.
- Build and integration tests **have not been executed in this environment**. Verify compile, migration and authorization with your environment.
