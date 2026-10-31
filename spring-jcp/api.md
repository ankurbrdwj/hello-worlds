# API: Entitlement & Metering Service (v1)

Base path: `/v1`. A breaking change gets a new prefix (`/v2`); additive changes (new optional fields) stay in `/v1` and clients must ignore unknown fields (FR-6).

Errors use one shape: `{"code": "QUOTA_EXCEEDED", "message": "..."}`.

## Ingestion (upstream systems)

| Method & path | Purpose | Success | Errors |
|---|---|---|---|
| `POST /v1/purchases` | Record a purchase/licence event (FR-1.1) | `201` first time, `200` replay of same `eventKey` | `400` malformed |
| `POST /v1/customers/{customerId}/seats` | Change seat count by a delta (FR-1.2) | `200` with new seat total | `400`, `404` unknown customer |
| `POST /v1/usage-events` | Record consumption, idempotent on `eventKey` (FR-2, FR-3) | `201` recorded, `200` duplicate (replays the original receipt, no change) | `409` quota exceeded or no entitlement, `422` same `eventKey` with different content |

`POST /v1/usage-events` body:

```json
{ "eventKey": "e-8812", "customerId": "acme", "userId": "dev42",
  "feature": "ai-credits", "units": 3, "occurredAt": "2026-10-05T10:01:00Z" }
```

Response (`201` and `200`): `{"eventKey": "e-8812", "usedAfter": 3}`, where `usedAfter` is the running total right after the original event was counted. A replay returns the original value, not the current total.

Rules: `eventKey` is the idempotency key. A duplicate (identical content) returns `200` with the original receipt and changes nothing. The same key with different content returns `422`. A rejected (`409`) event does not burn its key. Events may arrive in any order. Usage equal to the quota is accepted; one unit over is `409`.

## Queries (other JetBrains teams)

| Method & path | Purpose |
|---|---|
| `GET /v1/customers/{customerId}/entitlements` | What the customer may use now, with quota remaining (FR-1.3) |
| `GET /v1/customers/{customerId}/usage?feature=&from=&to=` | Recorded consumption for a period |

## Operations (internal team)

| Method & path | Purpose |
|---|---|
| `POST /v1/admin/reconciliations?repair=true` | Compare every `entitlement.used` with the sum of its recorded events (FR-4). Runs synchronously and returns `200` with the drift found. `repair=false` is a dry run that changes nothing; the default repairs |
| `PUT /v1/admin/migrations/{name}/phase` | Set phase: `OLD_ONLY`, `DUAL_WRITE`, `NEW_READ`, `NEW_ONLY` (FR-5) |
| `POST /v1/admin/migrations/{name}/backfill` | Start backfill |
| `GET /v1/admin/migrations/{name}/verification` | Mismatch count between old and new stores |

Reconciliation response: `{"drifted": [{"customerId": "acme", "feature": "ai-credits", "recordedUsed": 99, "actualUsed": 3}]}`. `actualUsed` (the sum of events) is the source of truth. Repair locks each counter row, recomputes the sum under the lock, then writes it, so it is safe to run repeatedly and while events are being ingested.

## Build order (TDD, one failing test at a time)

1. `POST /v1/usage-events`: first call `201`, repeat `200`, total counted once.
2. Same endpoint: usage equal to quota accepted, one over `409`.
3. Same endpoint under concurrency against real Postgres.
4. Entitlements query, then purchases/seats.
5. Reconciliation, then migration, then contract tests.
