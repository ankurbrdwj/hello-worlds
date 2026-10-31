# Implementation status

Spec: [requirements.md](requirements.md). Contract: [api.md](api.md). Last updated 2026-10-08: 16 tests, all green.

Run: `cd spring-jcp && ./gradlew test` (Docker must be running; tests start a real Postgres with Testcontainers).

Stack: Java source level 8 (`--release 8`, tests run on JDK 21), Spring Boot 2.7.18 (last line that supports Java 8), Postgres 16, Flyway, plain JDBC.

## Requirement to proof

Test classes are in `src/test/java/com/ankur/brains/jcp/usage/`: `UsageIngestionTest` (UI) and `ReconciliationTest` (RT).

| Req | Status | Proven by |
|---|---|---|
| FR-1 Entitlements (purchases, seats, query) | Not started. The `entitlement` table exists; tests insert rows directly | |
| FR-2.1 Usage event with unique `eventKey` | Done | UI `acceptedEventIsPersistedAndCounted` |
| FR-2.2 Idempotent ingestion | Done | UI `duplicateEventKeyIsAcceptedOnceAndReplayedAsOk`, `sameEventKeySentConcurrentlyIsRecordedExactlyOnce` (16 threads), `secondInstanceOrRestartedInstanceDoesNotCountAnAlreadyRecordedEvent`, `sameEventKeyWithDifferentContentIsRejectedAsUnprocessable`, `duplicateReplaysTheOriginalResultNotTheCurrentTotal` |
| FR-2.3 Order independence | Done (pinning test) | UI `eventsArrivingOutOfOrderGiveTheSameTotalAsInOrder` |
| FR-2.4 Dead-letter path | Not started (HTTP only; a malformed body gets Spring's default `400`) | |
| FR-2.5 Retries with backoff | Not started | |
| FR-3.1 Quota never exceeded under concurrency | Done | UI `parallelDistinctEventsNeverExceedTheQuota` (30 threads, quota 10) |
| FR-3.2 Boundary `<=` | Done | UI `usageEqualToQuotaIsAcceptedAndOneUnitOverIsConflict` |
| FR-3.3 Atomic record + counter | Done | UI `failureWhileCountingLeavesNoHalfWrittenEvent`, `failureAfterTheCounterWasIncrementedRollsBackTheCounterToo` (failures injected with Postgres triggers) |
| FR-4 Reconciliation (detect, repair, rerun, under traffic) | Done | RT `dryRunReportsDriftWithoutChangingTheCounter`, `repairFixesTheCounterAndASecondRunFindsNothing`, `repairAlsoRaisesACounterThatLostAnUpdate`, `repairDoesNotLoseAnEventCommittedWhileReconciliationWaitsOnTheRow` |
| FR-5 Live migration | Not started | |
| FR-6 Versioned API | Partial: `/v1` prefix only; no contract tests | |
| NFR-1 Invariants, metrics, alerts | Not started. Reconciliation is the invariant check, but nothing exposes it as a metric | |
| NFR-2 Postgres discipline | Partial: row lock + READ COMMITTED, index in `V3`, online-safe `V2` | see decisions |
| NFR-3 Unreliable upstreams | Partial: duplicates, reordering and key reuse handled; delayed and failed delivery are not | |
| NFR-4 Traceability (logs, trace ids) | Not started | |
| NFR-5 Real-dependency tests | Done: Testcontainers Postgres, concurrency tests, mutation-checked | |

## Design decisions

- **Idempotency key = primary key of `usage_event`.** `INSERT ... ON CONFLICT (event_key) DO NOTHING`; a conflict is a duplicate, or a key reuse if the stored content differs.
- **Quota check and increment are one statement.** `UPDATE entitlement SET used = used + ? WHERE ... AND used + ? <= quota RETURNING used`. Postgres locks the row, so concurrent requests queue and the check cannot be raced. A rejected request rolls back its event insert, so the key stays free.
- **Source of truth is the events.** `entitlement.used` is a derived counter; reconciliation rebuilds it from `SUM(units)`.
- **Repair locks first, recomputes second** (`SELECT ... FOR UPDATE`, then the sum, then the write, in one transaction per entitlement). Computing the sum before taking the lock loses events committed in between.
- **Migrations:** `V1` tables, `V2` adds nullable `used_after` (metadata-only change, no table rewrite, old rows stay valid), `V3` index for the reconciliation sum.

## Findings worth remembering

- `@Transactional` is **silently ignored on non-public methods** with Spring's proxies. The first versions ran with no transaction at all until a test failed with `NoTransactionException`. `UsageService.record` is public on purpose.
- Removing `@Transactional` turns four tests red (mutation check), so the transaction tests do guard it.
- Docker Engine 28+ rejects old Testcontainers clients; the build uses Testcontainers 1.21.3.
- Tests share one database, so test data must not collide. `eventKey` is unique across everything, which caused a real `422` between two test classes.

## Known gaps and open questions

- A missing entitlement returns `409`, not the `404` first drafted in `api.md`. `409` and `422` responses have no body, so the `{"code": ...}` error shape is not implemented.
- `eventKey` is global. A real system would probably scope it by upstream source.
- Isolation level is the default (READ COMMITTED) and correctness relies on the row lock. Not yet compared against SERIALIZABLE with retry.
- Reconciliation runs synchronously; runs are not stored and there is no `GET` for past runs.

## Next

FR-5 live migration (dual write, backfill, verify, reversible flag), then FR-1 entitlements, then metrics for NFR-1.
