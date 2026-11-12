# FXAI Infrastructure

The Docker Compose configuration provides TimescaleDB, a PostgreSQL-compatible database with time-series support, and Redis for short-lived latest-price caching. Copy the variables from `.env.example` into an untracked `.env` file at the repository root and replace the example password.

Start both services from the repository root:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d database redis
```

The backend connects with `FXAI_DATABASE_URL`, `FXAI_DATABASE_USERNAME`, and `FXAI_DATABASE_PASSWORD`. Flyway applies every migration under `backend/src/main/resources/db/migration` when the application starts. Hibernate validates the mapped entities against that schema; it does not create or update production-style schemas.

The backend connects to Redis with `FXAI_REDIS_HOST` and `FXAI_REDIS_PORT`. Connection and command timeouts default to `500ms` and can be changed with `FXAI_REDIS_CONNECT_TIMEOUT` and `FXAI_REDIS_TIMEOUT`. Redis is not persisted by Compose because it is an expendable cache, not a system of record.

To stop the database while retaining its data:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml down
```

To reset the local database completely, including all persisted alerts and candles:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml down -v
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d database
```

The next backend startup recreates the schema through Flyway. Removing the named volume is destructive and cannot be undone.
