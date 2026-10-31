# Requirements: Entitlement & Metering Service (practice project)

Context: the JetBrains JCP Licensing & Quotation role is a correctness-first backend job on the money path. The team decides what each customer is entitled to use and bills for it, so errors cost revenue. The emphasis is distributed-systems discipline, not frameworks or algorithms. This project exists to practise that discipline and to have a concrete example for each point in the job posting.

## Scope

A small service that:
- ingests purchase/licence events and usage events from upstream systems,
- decides what a customer is entitled to use,
- enforces quotas,
- records consumption accurately enough to bill on.

Stack: Java or Kotlin on the JVM, PostgreSQL, Kafka. Integration tests run against real dependencies (Testcontainers), not mocks. Each module has its own Gradle build; `cd spring-jcp` first.

## Guiding principle

Correctness beats throughput. When unsure, reject, retry or flag; never guess silently. Every requirement below must be demonstrated by an automated test, and the most important ones by a test against real Postgres.

## Functional requirements

### FR-1 Entitlements
- FR-1.1 Ingest purchase events and derive per-customer entitlements.
- FR-1.2 Support seat-based plans and usage-based plans, and combinations of both.
- FR-1.3 An entitlement query returns what the customer may use now, with the quota remaining.

### FR-2 Usage ingestion
- FR-2.1 Accept usage events carrying a unique event key.
- FR-2.2 Ingestion is idempotent: the same event key is recorded once, including when duplicates arrive concurrently.
- FR-2.3 Results do not depend on arrival order. Late and out-of-order events produce the same totals.
- FR-2.4 A malformed or unprocessable event goes to a dead-letter path and does not block the stream.
- FR-2.5 Retries use backoff and never create duplicates.

### FR-3 Quota enforcement
- FR-3.1 With a quota of K and N parallel requests, no more than K succeed.
- FR-3.2 The boundary is explicit: usage equal to the quota is allowed, one more is rejected (`<=`, not `<`).
- FR-3.3 Recording usage and updating the quota counter are one transaction. If either fails, neither is applied.

### FR-4 Reconciliation
- FR-4.1 A reconciliation job compares the counters with the sum of recorded events.
- FR-4.2 Drift is detected, reported, and repaired.
- FR-4.3 Reconciliation is safe to run repeatedly and while traffic is flowing.

### FR-5 Live migration
- FR-5.1 Migrate data from an old schema to a new one without downtime, using a feature flag, dual writes, a backfill and verification.
- FR-5.2 During dual write, both stores agree.
- FR-5.3 After the backfill, a verification pass reports zero mismatches.
- FR-5.4 The flag can be flipped forward and back without losing data.

### FR-6 Internal API
- FR-6.1 The API is versioned.
- FR-6.2 A new version stays backward compatible with old clients.
- FR-6.3 Contract tests pin the response shapes.

## Non-functional requirements

- NFR-1 Observability: define invariants (for example, the sum of events equals the counter), expose metrics for them, and alert on violation so silent divergence is caught before customers see it.
- NFR-2 Database: correct use of transactions, isolation level and locking, with the chosen level justified in writing. Indexes support the hot queries. Schema changes are online-safe.
- NFR-3 Reliability: the service behaves correctly when upstream systems are unreliable (duplicate, delayed, reordered, or failed delivery).
- NFR-4 Debuggability: logs and traces carry the event key and customer id so an incident can be traced end to end.
- NFR-5 Testing: tests are treated as a design tool. Integration tests use real Postgres and Kafka. Concurrency tests exist for FR-2.2 and FR-3.1.

## Priority

Highest first, as these map most directly to the job posting:

1. FR-2.2 idempotency
2. FR-3.1 concurrent quota enforcement
3. FR-4 reconciliation
4. FR-5 live migration
5. Everything else

## Out of scope

- UI, authentication, real payment or billing integration.
- Performance tuning beyond what correctness requires.

## Open questions

- Which isolation level and locking strategy for quota checks (row lock, advisory lock, or serializable with retry)? Decide with a test that shows the failure of the naive approach first.
- Idempotency key storage: a unique constraint on the event table versus a separate dedupe table.
- Whether late events past a cut-off should be rejected, or accepted and corrected by reconciliation.
