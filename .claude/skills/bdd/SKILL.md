---
name: bdd
description: Generate Gherkin feature files and Java Cucumber step definitions; optionally deploy multiple services as Docker containers and run BDD against them
---

Generate BDD test artifacts for a Spring Boot Cucumber module. Supports two modes:

- **unit** (default) — `@Autowired` Spring beans, H2 in-memory DB, no containers
- **docker** — HTTP via `RestTemplate`, live containers, real databases

## Usage

```
/bdd [module] [feature description]
/bdd --docker [module] [services...] [feature description]
```

- `module` — BDD test module directory, e.g. `spring-bdd`
- `services` — (docker mode only) one or more sibling service directories to containerise, e.g. `user-service price-alert`
- `feature description` — plain-English description of the behaviour to test

If any argument is missing, ask the user before proceeding.

---

## Unit mode

### Step 1 — Gather module context

Read from the target module:

1. `build.gradle` — extract `group` for the base package (e.g. `com.ankur`)
2. `src/test/resources/features/*.feature` — style references
3. `src/test/java/**/*StepDefinitions.java` — code style references (prefer `UserStepDefinitions.java`)
4. `src/main/java/` class names — available `@Autowired` types

### Step 2 — Generate Gherkin feature file

Follow the existing feature file header style (`Feature` → `As a` / `I want` / `So that`). Cover happy path, errors, and state transitions. Name it `<snake_case>.feature`.

### Step 3 — Generate step definitions

Follow `UserStepDefinitions.java` exactly:
- Package: `<group>.bdd.steps`
- `@Autowired` fields for services and repositories
- Instance fields for scenario-scoped state
- `@After` that nulls all state and calls `deleteAll()` on every repository
- `{string}` / `{long}` / `{double}` placeholders in step annotation text
- AssertJ assertions only

### Step 4 — Check conflicts, write, run

Check whether files already exist; show a diff and ask before overwriting. Write:

| Artifact | Destination |
|---|---|
| `<name>.feature` | `<module>/src/test/resources/features/<name>.feature` |
| `<Name>StepDefinitions.java` | `<module>/src/test/java/<pkg/path>/steps/<Name>StepDefinitions.java` |

Then run:
```bash
cd <module> && ./gradlew test
```

Report pass/fail. On failure show the failing scenario and step; offer to fix and re-run.

---

## Docker mode

Runs BDD scenarios against real, containerised services. Step definitions use HTTP (`RestTemplate`) rather than injected beans.

### Step 1 — Gather context from every service

For each service in `services`:

1. Read `build.gradle` — note `group`, `version`, server port from `src/main/resources/application.properties`
2. Read REST controllers under `src/main/java/` — note endpoint paths and request/response types
3. Read existing `docker-compose.yml` or `compose.yaml` — note infrastructure dependencies (Postgres, Redis, Kafka)

Also read the BDD module's existing HTTP-based step definitions (e.g. `UserPriceAlertSteps.java`) as a style reference.

### Step 2 — Generate Gherkin feature file

Same rules as unit mode but scenarios describe cross-service behaviour. Use concrete quoted values and avoid referencing internal bean state.

### Step 3 — Generate HTTP step definitions

Follow `UserPriceAlertSteps.java` exactly:
- Package: `<bdd-module-group>.bdd.steps`
- One `@Value("${<service-name>.base-url}")` field per service (e.g. `${user-service.base-url}`)
- `@Autowired RestTemplate restTemplate`
- Scenario state in instance fields; `@After` cleans up via DELETE calls then nulls all fields
- Use `restTemplate.postForEntity`, `exchange`, `getForObject`, `delete`
- Catch `HttpClientErrorException` for expected error scenarios; assert on `response.getStatusCode()`

### Step 4 — Generate `application-docker.properties`

Write `<module>/src/test/resources/application-docker.properties` mapping each service to its container port:

```properties
user-service.base-url=http://localhost:<port>
price-alert.base-url=http://localhost:<port>
```

Use the server ports discovered in Step 1. If a port conflicts with an existing service in CLAUDE.md, pick the documented port.

### Step 5 — Generate `docker-compose.bdd.yml`

Create a Docker Compose file at `<module>/docker-compose.bdd.yml` that:

1. For each service:
   - Builds from `../<service-dir>` using a `Dockerfile` (check if one exists; if not, note that the user must add it — see Step 6)
   - Sets `SPRING_PROFILES_ACTIVE=docker` and all `SPRING_DATASOURCE_*` / `SPRING_KAFKA_*` env vars the service needs
   - Maps the service's server port to the same host port
   - Declares a `healthcheck` using `curl -f http://localhost:<port>/actuator/health` with `interval: 10s`, `retries: 10`
   - Adds `depends_on` with `condition: service_healthy` for its infrastructure

2. Includes all infrastructure the services collectively need (Postgres, Redis, Kafka) — merge from the existing compose files
3. Uses a dedicated Docker network `bdd-net`

```yaml
# example shape
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: bdd
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      retries: 5
    networks: [bdd-net]

  user-service:
    build:
      context: ../user-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bdd
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres123
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 10s
      retries: 10
    networks: [bdd-net]

networks:
  bdd-net:
```

### Step 6 — Check for Dockerfiles

For each service directory check whether a `Dockerfile` exists. If any are missing, generate a standard multi-stage Spring Boot Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Write it to `<service-dir>/Dockerfile`. Show what was generated and ask for confirmation before writing.

### Step 7 — Add `spring-boot-starter-actuator` if missing

For each service, check `build.gradle` for `spring-boot-starter-actuator`. If absent, add it — the healthcheck depends on `/actuator/health`. Show the diff and ask for confirmation.

### Step 8 — Check conflicts, write all files

Show every file that will be created or modified and ask for one confirmation before writing anything:

| Artifact | Destination |
|---|---|
| `<name>.feature` | `<module>/src/test/resources/features/<name>.feature` |
| `<Name>StepDefinitions.java` | `<module>/src/test/java/<pkg/path>/steps/<Name>StepDefinitions.java` |
| `application-docker.properties` | `<module>/src/test/resources/application-docker.properties` |
| `docker-compose.bdd.yml` | `<module>/docker-compose.bdd.yml` |
| `Dockerfile` (per missing service) | `<service-dir>/Dockerfile` |

### Step 9 — Build and start containers

```bash
cd <module> && docker-compose -f docker-compose.bdd.yml build
cd <module> && docker-compose -f docker-compose.bdd.yml up -d
```

Wait for all services to be healthy:
```bash
docker-compose -f <module>/docker-compose.bdd.yml ps
```

Poll every 5 seconds, up to 2 minutes. Report each service's health status. Stop and print logs if any service fails to become healthy:
```bash
docker-compose -f <module>/docker-compose.bdd.yml logs <service-name>
```

### Step 10 — Run BDD tests against containers

```bash
cd <module> && ./gradlew test -Dspring.profiles.active=docker
```

Report pass/fail:
- **Pass** — state scenario count, confirm all containers healthy during the run
- **Fail** — show failing scenario and step, print relevant container logs, offer to fix the step definitions and re-run

### Step 11 — Tear down

```bash
cd <module> && docker-compose -f docker-compose.bdd.yml down -v
```

Always tear down, even if tests failed.