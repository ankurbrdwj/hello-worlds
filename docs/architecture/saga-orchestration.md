---
title: "Saga Pattern — Orchestration"
type: architecture
tags: [saga, orchestration, kafka, postgres, redis]
sources: ["raw/saga-orchestration-design.md"]
related: ["[[outbox-pattern]]", "[[saga-choreography]]", "[[saga-orchestration-vs-choreography]]", "[[architecture/overview]]"]
created: 2026-10-27
updated: 2026-10-27
confidence: medium
---

# Saga Pattern — Orchestration

A **design** (not yet confirmed as implemented) for a distributed "Place Order" transaction where a central `saga-orchestrator` owns the workflow, sends commands, and drives compensation.

## Business flow
| # | Step | Service | Compensation |
|---|---|---|---|
| 1 | Create order `PENDING` | `order-service` / `outbox-pattern` | `CancelOrder` |
| 2 | Reserve credit | `user-service` | `ReleaseCredit` |
| 3 | Post charge | `billing-period` (Redis) | `ReverseCharge` |
| 4 | Provision alert quota | `price-alert` | `RevokeAlertQuota` |
| 5 | Notify customer | `server-sent-events` | none (send `NotifyFailure`) |

Compensation runs in **reverse order** from the last completed step.

## Components
- New module `saga-orchestrator` (Boot 3.5.x, Java 21): `POST /sagas/place-order`, `GET /sagas/{id}`, `POST /sagas/{id}/retry`, timeout sweeper.
- Kafka topics `saga.<service>.commands` / `.replies`, keyed by `sagaId`, 3 partitions.
- State in Postgres `5434`; idempotency keys, locks and timers in Redis `6379`.

## State machine
`STARTED → RUNNING_FORWARD → COMPLETED`, or `→ COMPENSATING → COMPENSATED | COMPENSATION_STUCK`.

## Reliability
- **Atomic decide-then-send:** commands go to `saga_outbox` in the same transaction as the state update; see [[outbox-pattern]].
- **Idempotency:** key `{sagaId}-{stepName}-{direction}` via Redis `SET NX`.
- **Single-writer lock** per saga (Redis) plus optimistic `version` column.
- **Timeouts:** `deadline_at` per step (default 30 s); sweeper retries, then compensates.
- **Compensations must always succeed;** impossible ones raise a manual-intervention ticket.

## Trade-offs
Pros: workflow in one place, central status/retry, dumb participants, easy stall detection.
Cons: a new service to run, god-service risk, extra hop of latency.
Choose it for flows that branch, have many steps, or need operator control.
Alternative without a coordinator: [[saga-choreography]]; side-by-side in [[saga-orchestration-vs-choreography]].
