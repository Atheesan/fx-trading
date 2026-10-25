# FXAI Infrastructure

The Docker Compose configuration provides TimescaleDB, a PostgreSQL-compatible database with time-series support. Copy the database variables from `.env.example` into an untracked `.env` file at the repository root and replace the example password.

Start the database from the repository root:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d database
```

The backend connects with `FXAI_DATABASE_URL`, `FXAI_DATABASE_USERNAME`, and `FXAI_DATABASE_PASSWORD`. Flyway applies every migration under `backend/src/main/resources/db/migration` when the application starts. Hibernate validates the mapped entities against that schema; it does not create or update production-style schemas.

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
