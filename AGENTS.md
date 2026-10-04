# FXAI Project Guidelines

These instructions apply to the entire repository. More specific `AGENTS.md` files in subdirectories may add local conventions but must not weaken the project scope, security rules, or V1 exclusions defined here.

## Project purpose

FXAI V1 is a professional, real-time Forex market monitoring dashboard. The project also serves as a practical environment for learning modern full-stack, event-driven, cloud-native engineering.

Keep V1 focused on market monitoring. Do not add features outside that scope unless the user explicitly changes the project scope.

## Technology direction

### Backend

- Java 25 LTS
- Spring Boot
- Spring Web
- Spring WebSocket
- Spring Security later, when explicitly requested
- Gradle
- PostgreSQL
- TimescaleDB where appropriate
- Redis
- Apache Kafka

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS
- TanStack Query
- Zustand only where global client state is justified
- Lightweight Charts for financial charts

### Infrastructure and observability

- Docker
- Docker Compose
- Prometheus
- Grafana

### Testing

- JUnit
- Mockito
- Spring Boot Test
- Testcontainers
- Vitest
- React Testing Library
- Playwright later, when explicitly requested or justified by the requested work

## Architecture principles

- Prefer a modular monolith initially. Do not introduce microservices without an explicit architectural decision from the user.
- Maintain clean separation of responsibilities and clear module boundaries.
- Use interfaces around external providers so market-data providers remain replaceable.
- Keep provider-specific and vendor-specific logic out of the domain layer.
- Use DTOs at external API boundaries.
- Do not expose persistence entities directly through REST APIs.
- Use `BigDecimal` for financial numeric values where precision matters.
- Store timestamps in UTC.
- Use ISO-8601 timestamp representations in external APIs.
- Avoid overengineering and premature abstractions.
- Keep classes and components reasonably small and cohesive.
- Avoid duplication.
- Do not silently change the architecture. Explain and obtain direction for material architectural changes.

## V1 exclusions

Do not implement any of the following in V1:

- Real-money trading
- Paper trading
- Order execution
- Automated trading
- Machine-learning predictions
- LLM functionality
- Retrieval-augmented generation (RAG)
- AI agents
- Portfolio management

## Security and configuration

- Never hard-code secrets or credentials.
- Use environment variables for credentials and environment-specific configuration.
- Do not commit real secrets or populated local secret files.
- Add validation at system boundaries.
- Provide meaningful error handling without exposing sensitive implementation details.
- Add Spring Security only when the requested work calls for it; it is not part of the initial implementation by default.

## Working rules

For every task:

1. Inspect the existing repository before making changes.
2. Follow established project conventions.
3. Briefly explain the intended changes before editing files.
4. Do not rewrite working code unnecessarily.
5. Preserve unrelated user changes in the working tree.
6. Keep the implementation limited to the requested feature or fix.
7. Do not implement unrequested features.
8. Add or update tests for important behavior.
9. Run the relevant tests after modifications.
10. Fix failures caused by the changes.
11. If a relevant test cannot be run, report the reason clearly.
12. Use validation and meaningful error handling where applicable.
13. Never hard-code secrets; introduce documented environment variables for credentials and configuration.
14. Do not silently change architecture or technology choices.

## Task completion report

At the end of every task, report:

- Files created
- Files changed
- Architectural decisions
- Tests executed
- Remaining issues

If a section has no entries, explicitly state `None`.
