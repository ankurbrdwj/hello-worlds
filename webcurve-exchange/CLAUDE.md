# webcurve-exchange

## What We Are Building

**webcurve-exchange** is the central orchestration layer of a full-stack financial exchange platform — think a simplified crypto/stock exchange. The goal is to wire together all the sibling microservices in this mono-repo into a cohesive, production-grade trading system.

The platform allows users to:
- View real-time price feeds and candlestick charts for tradeable instruments (ISINs)
- Place and process orders
- Receive price alerts when thresholds are crossed
- Get billed on a weekly/periodic basis for transactions
- Interact through a single API gateway with rate limiting and circuit breaking

---

## Ecosystem — Sibling Services

All services live under `../` (i.e. `/hello-worlds/`):

| Service | Group | Purpose |
|---|---|---|
| `api-gateway` | `com.ankur.gateway` | Spring Cloud Gateway (WebMVC) — single entry point, rate limiting (Redis + Resilience4j), circuit breaking |
| `market-feed` | `com.ankur.exchange` | WebSocket server — streams live instrument quotes and price updates (random-walk generator) |
| `candlesticks` | `com.ankur` | Consumes market-feed quotes via WebSocket, aggregates into 1-min OHLC candlestick data, exposes REST + WebSocket |
| `price-alert` | `com.ankur` | Kafka consumer — fires alerts (email/SMS/push/webhook) when instrument prices cross user-defined thresholds |
| `order-service` | `com.ankur.orderservice` | Order processing engine — factory pattern, dual constraint (maxOrders + maxTime), streaming processor |
| `user-service` | `com.ankur.userservice` | User management REST API |
| `billing-period` | `com.ankur` | Weekly billing period calculator — REST endpoint, Redis-cached |
| `rate-limiter` | `com.ankur` | Standalone rate limiting implementations (token bucket, sliding window log, leaky bucket, counter) |
| `outbox-pattern` | `com.ankur.outbox` | Transactional outbox — reliable event publishing to Kafka via PostgreSQL outbox table |
| `server-sent-events` | `com.ankur` | SSE-based real-time notifications — Kafka consumer + PostgreSQL, inspired by fintech 3DS verification flow |

---

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.3** (webcurve-exchange itself)
- **Spring Web MVC** + **Spring Data REST**
- **Lombok**
- **Gradle** (build tool)
- **Docker Compose** (dev services — currently empty, needs services added)

### Infrastructure used across the ecosystem
- **Apache Kafka** — price alerts, outbox event publishing, SSE notifications
- **Redis** — API gateway rate limiting, billing-period caching
- **PostgreSQL** — outbox pattern persistence, SSE event store
- **WebSocket** — market-feed quote streaming, candlestick live updates

---

## Project Structure

```
webcurve-exchange/          ← YOU ARE HERE (orchestration layer)
├── src/
│   ├── main/
│   │   ├── java/com/ankur/webcurve/
│   │   │   └── WebcurveExchangeApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/ankur/webcurve/
│           └── WebcurveExchangeApplicationTests.java
├── build.gradle
├── compose.yaml            ← empty, needs Docker services
└── settings.gradle

hello-worlds/               ← mono-repo root
├── api-gateway/
├── market-feed/
├── candlesticks/
├── price-alert/
├── order-service/
├── user-service/
├── billing-period/
├── rate-limiter/
├── outbox-pattern/
├── server-sent-events/
├── spring-kafka/
├── spring-redis/
└── webcurve-exchange/
```

---

## Common Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean
```

---

## Notes

- `compose.yaml` is currently empty — add Docker services (PostgreSQL, Redis, Kafka, Zookeeper) before running; Spring Boot Docker Compose integration requires at least one service.
- This service is the scaffold for the exchange's core domain logic — trading engine, order book, instrument registry.
- Group: `com.ankur`, artifact: `webcurve-exchange`.