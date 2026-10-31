---
title: "Saga Pattern — Choreography"
type: architecture
tags: [saga, choreography, kafka, redis]
sources: ["raw/saga-choreography-design.md"]
related: ["[[saga-orchestration]]", "[[saga-orchestration-vs-choreography]]", "[[outbox-pattern]]", "[[architecture/overview]]"]
created: 2026-10-27
updated: 2026-10-27
confidence: medium
---

# Saga Pattern — Choreography

A **design** for the same "Place Order" flow as [[saga-orchestration]], with **no coordinator**: each service reacts to Kafka events, does local work, and emits the next event.

## Event chain and its reverse
Forward: `OrderCreated → CreditReserved → ChargePosted → AlertQuotaProvisioned → NotifySuccess`.
A failing service emits a `*Failed` event; services that already completed their step emit a compensation-requested event, producing a reverse cascade (e.g. `ChargeFailed → CreditReleaseRequested → CreditReleased → OrderCancelledRequested → OrderCancelled → NotifyFailure`).
No shared saga table; the only shared state is the `sagaId` correlation id in every event.

## Reliability (enforced locally by every participant)
- **Transactional outbox is mandatory** — a per-service `saga_outbox` table and `@Scheduled` `OutboxProcessor`, copied from [[outbox-pattern]]. `billing-period` is Redis-only, so the source recommends giving it a small Postgres for the outbox.
- **Idempotent consumption:** Redis `SET saga:seen:{group}:{sagaId}:{eventType} NX EX 604800`; on duplicate, re-publish the stored outcome.
- **Ordering:** single topic keyed by `sagaId`.
- **Stalled saga detection** (the hard part): recommended `saga-watchdog` consumer tracking last-seen event per saga in Redis; alternatives are per-step deadline events, or nothing.
- **Compensation must not fail;** impossible ones emit `ManualInterventionRequired`.
- **Rule:** each service compensates only on the failure/compensation event from the step *immediately downstream* of its own.

## Trade-offs
Pros: no new coordinator, loose coupling, lower happy-path latency.
Cons: workflow scattered across services, stall detection hard, status queries hard, compensation matrix easy to get wrong.
Choose it for short, stable, linear chains. Comparison: [[saga-orchestration-vs-choreography]].
