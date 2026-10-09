# Local environment setup

From the repository root:

```sh
cp .env.example .env
openssl rand -hex 32
```

Paste the generated value into `JWT_SECRET` in `.env`, replace the placeholder database and bootstrap passwords, then run:

```sh
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d
cd backend
./gradlew bootRun
```

Spring Boot imports `../.env` when launched from `backend/`. Run from that directory. The `.env` syntax is `KEY=value`, not `export KEY=value`. Avoid shell substitutions or quoted values with special escapes. After first boot, remove the bootstrap password and restart. Never commit `.env`.
