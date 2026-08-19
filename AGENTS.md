Mono-repo of ~20 independent Spring Boot microservices, each with a self-contained Gradle build. No root Gradle wrapper — always `cd` into the module first.

## Commands

```bash
./gradlew build
./gradlew test
./gradlew bootRun
./gradlew clean build
./gradlew test --tests "com.ankur.tdd.spring.controller.OrderControllerTest"
./gradlew test --tests "com.ankur.tdd.spring.controller.OrderControllerTest.shouldCreateOrder"
```

Modules needing infra have a `docker-compose.yml` — start it before `bootRun`.

`spring-redis/processor/` is a Shadow JAR: `cd spring-redis/processor && ./gradlew shadowJar`

## Architecture

`webcurve-exchange` orchestrates a financial exchange platform:

| Module | Role | Key Tech |
|---|---|---|
| `api-gateway` | Entry point, rate limiting, circuit breaking | Spring Cloud Gateway, Resilience4j, Redis |
| `market-feed` | Live instrument quotes via WebSocket | WebSocket |
| `candlesticks` | Aggregates market-feed quotes into 1-min OHLC | Postgres |
| `price-alert` | Fires alerts when prices cross thresholds | Kafka, Postgres, WebSocket |
| `order-service` | Order processing (maxOrders + maxTime constraints) | Factory pattern, Kafka Streams |
| `user-service` | User management REST API | Spring Data REST |
| `billing-period` | Billing period calculator | Redis |
| `rate-limiter` | Rate limiting algorithms | Token bucket, sliding window, leaky bucket |
| `outbox-pattern` | Reliable event publishing via Postgres outbox | Spring Data JDBC, Postgres |
| `server-sent-events` | SSE notifications | Kafka, Postgres |

Data flow: `market-feed` → WebSocket → `candlesticks` / `price-alert`; writes → `outbox-pattern` → Kafka → consumers; all external traffic → `api-gateway`.

## Local Ports

| Service | Port |
|---|---|
| candlesticks Postgres | 5432 |
| spring-postgres Postgres | 5442 |
| outbox-pattern Postgres | 5434 |
| price-alert Postgres | 5435 |
| spring-redis Redis | 6379 |
| spring-redis app | 8080 |
| spring-postgres app | 6080 |
| user-service app | 8081 |
| price-alert app | 9095 |
| price-alert Conduktor | 9080 |

Dev environment (user-service + price-alert + Postgres + Kafka): `docker-compose -f docker-compose.dev.yml up -d --build`

## Study Modules

- **`spring-postgres`** — SQL chapters in `src/main/resources/static/` (`chapter1.sql`–`chapter11.sql`). Seed once with `seed.sql`. New chapters: follow header format from chapters 4–7 (plain-text block, then `-- 1. ...` examples).
- **`spring-kafka`** — Kafka fundamentals + crypto trade topology.
- **`spring-saml`** — SAML2: per-tenant `RelyingPartyRegistration`, JIT provisioning, SCIM, cert rotation.
- **`spring-tdd`**, **`spring-bdd`** — TDD/BDD with H2, no external infra needed.
- **`spring-modulith`** — three sub-modules: `auth/`, `gateway/`, `adoption/`.

`spring-redis` OOM demo:
```bash
docker-compose --profile oom up    # CACHE_FETCH_CONCURRENCY=16 → triggers OOM
docker-compose --profile fixed up  # CACHE_FETCH_CONCURRENCY=4  → backpressure fix
```
Heap dumps → `./dumps/`.

## Java & Spring Boot Versions

Check each module's `build.gradle` — not standardised:
- Newer (spring-postgres, spring-redis, api-gateway, outbox-pattern, webcurve-exchange): **Spring Boot 4.0.x**, Java 21
- Older: Spring Boot 3.3.x–3.5.x, Java 21 or 22
- `spring-redis/processor/`: Java 8, no Spring