# Resilience4j Capabilities

Resilience4j is a lightweight fault-tolerance library providing a set of independent modules, each implemented as a decorator around a function/supplier call. They can be combined (e.g. CircuitBreaker + Retry + RateLimiter) around the same call.

## Modules

| Module | Purpose | Used in `api-gateway`? |
|---|---|---|
| **CircuitBreaker** | Stops calling a failing dependency once failure rate/slow-call rate crosses a threshold; opens the circuit and short-circuits calls for a wait duration, then trials a few calls (half-open) before fully closing again. States: `CLOSED` → `OPEN` → `HALF_OPEN`. | Dependency present (`spring-cloud-starter-circuitbreaker-resilience4j`) but **not wired up** in code yet |
| **RateLimiter** | Restricts the number of calls allowed in a time window (`limitForPeriod` / `limitRefreshPeriod`), rejecting or blocking (`timeoutDuration`) calls beyond the limit. | ✅ Yes — `Resilience4jRateLimiter.java`, in-memory, per-key limiter with `timeoutDuration=0` (non-blocking, immediate reject) |
| **Bulkhead** | Limits concurrent calls to a dependency, either via a fixed thread pool (`ThreadPoolBulkhead`) or a semaphore (`SemaphoreBulkhead`), to prevent one slow dependency from exhausting resources. | Not used |
| **Retry** | Automatically retries a failed call, with configurable max attempts, wait duration, and backoff (exponential/randomized). | Not used |
| **TimeLimiter** | Cancels/fails a call that exceeds a configured duration; typically wraps `CompletableFuture`-based calls. | Not used |
| **Cache** | Caches the result of a call so repeated calls with the same key skip the underlying function (rarely used standalone since caching is often handled elsewhere, e.g. Redis). | Not used |

## Combining modules

Resilience4j decorators can be stacked, typically in this order (outermost to innermost):

```
Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( call ) ) ) ) )
```

Order matters — e.g. put `CircuitBreaker` outside `Retry` so retries don't keep hammering an already-open circuit; put `RateLimiter` outermost if you want rate limiting to apply even to retried calls.

## Cross-cutting features

- **Registries** (`CircuitBreakerRegistry`, `RateLimiterRegistry`, etc.) — central place to create/retrieve named instances sharing a config, and to observe state transitions.
- **Event publishers** — every module exposes an `EventPublisher` (e.g. `onStateTransition`, `onCallRejected`) for logging/metrics hooks.
- **Metrics** — integrates with Micrometer for exporting call counts, failure rates, state, wait times, etc. (useful with Actuator/Prometheus).
- **Spring Boot integration** (`spring-cloud-starter-circuitbreaker-resilience4j`) — allows declarative usage via `@CircuitBreaker`, `@RateLimiter`, `@Retry`, `@Bulkhead`, `@TimeLimiter` annotations, and YAML/properties-based configuration (`resilience4j.circuitbreaker.instances.*`, etc.) instead of manual registry wiring.

## Current gateway usage

Only **RateLimiter** is actually used, and only manually via the registry API (not the Spring annotation/YAML style):

- `Resilience4jRateLimiter.java` — creates a per-key `RateLimiter` from a shared `RateLimiterConfig` (`limitForPeriod`, `limitRefreshPeriod`, `timeoutDuration=Duration.ZERO`).
- `RateLimiterConfig.java` / `RateLimiterService.java` — Spring config + interface selecting between this in-memory limiter and a Redis-backed distributed limiter via `gateway.ratelimiter.type` (`application.properties`).
- `GatewayRoutingConfig.java` — wires the selected limiter into gateway routes.

The `CircuitBreaker` dependency is on the classpath but has no code using it yet — a candidate for wrapping downstream route calls to `order-service`, `user-service`, etc.
