# FXAI Infrastructure

The Docker Compose configuration provides TimescaleDB, Redis for latest prices, and Apache Kafka 4.1.2 in KRaft mode. Kafka combines a broker and controller for local development; there is no ZooKeeper. Copy the variables from `.env.example` into an untracked `.env` file at the repository root and replace the example password.

Start the services from the repository root:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d database redis kafka
```

The backend connects with `FXAI_DATABASE_URL`, `FXAI_DATABASE_USERNAME`, and `FXAI_DATABASE_PASSWORD`. Flyway applies every migration under `backend/src/main/resources/db/migration` when the application starts. Hibernate validates the mapped entities against that schema; it does not create or update production-style schemas.

The backend connects to Redis with `FXAI_REDIS_HOST` and `FXAI_REDIS_PORT`. Connection and command timeouts default to `500ms` and can be changed with `FXAI_REDIS_CONNECT_TIMEOUT` and `FXAI_REDIS_TIMEOUT`. Redis is not persisted by Compose because it is an expendable cache, not a system of record.

The host-run backend connects using `FXAI_KAFKA_BOOTSTRAP_SERVERS=localhost:9092`. Containers on the Compose network use `kafka:19092`. `FXAI_KAFKA_PORT` changes the host listener port; update bootstrap servers to match. The external listener binds only to host loopback. Local listeners use PLAINTEXT; remote deployments must supply secured broker settings and multiple brokers/controllers. Kafka data persists in `fxai_kafka_data`. The fixed KRaft cluster ID is a local identifier, not a credential. Single-node replication is for development only.

Spring Kafka creates `market.price.tick.v1` with three partitions, replication factor one, and 24-hour retention on backend startup. Verify it with:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --describe --topic market.price.tick.v1
docker compose --env-file .env -f infrastructure/docker-compose.yml exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:19092 --all-groups --describe
```

`FXAI_KAFKA_REDIS_GROUP` is shared for cache updates. `FXAI_KAFKA_WEBSOCKET_GROUP` must be distinct and stable for each backend instance with local browser sessions. Retry intervals/attempts, logging, topic creation, and listener startup variables are listed in `.env.example`. See [architecture](../docs/architecture.md#kafka-price-tick-distribution) for the event contract, delivery limits, and replay behavior. Stop the affected consumer group before resetting its offsets for replay; replay only retained records.

Run `./gradlew test` and `./gradlew build` from `backend` (`gradlew.bat` on Windows). Testcontainers requires a running Docker Linux daemon; the Kafka integration test uses the same official KRaft image, real Redis, both consumer groups, JSON serialization, WebSocket delivery, and duplicate/out-of-order ticks. Other database tests also require Docker.

To stop the database while retaining its data:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml down
```

To reset all local persisted infrastructure data, including alerts, candles, Kafka ticks and consumer offsets:

```shell
docker compose --env-file .env -f infrastructure/docker-compose.yml down -v
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d database
```

The next backend startup recreates the schema through Flyway. Removing the named volume is destructive and cannot be undone.
