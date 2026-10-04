# FXAI

FXAI V1 is a real-time Forex market monitoring dashboard and a learning project for modern full-stack, event-driven, cloud-native engineering.

This initial monorepo contains buildable backend and frontend application shells only. Forex functionality and infrastructure integrations have not been implemented.

## Repository layout

```text
.
├── backend/         Spring Boot and Gradle application
├── frontend/        React, TypeScript, and Vite application
├── infrastructure/  Future Docker Compose and observability configuration
└── docs/            Architecture and project documentation
```

See [the architecture document](docs/architecture.md) for V1 boundaries and planned flows.

## Backend

The backend uses a Gradle Java 25 LTS toolchain. The Gradle wrapper downloads the required Gradle version.

```shell
cd backend
./gradlew test
./gradlew bootRun
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

To run with local browser CORS enabled:

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

The local frontend origin defaults to `http://localhost:5173` and can be changed with `FXAI_CORS_ALLOWED_ORIGIN`.

## Frontend

Requirements: a current Node.js release and npm.

```shell
cd frontend
npm install
npm run dev
```

Run `npm run build`, `npm test`, and `npm run lint` for validation.
