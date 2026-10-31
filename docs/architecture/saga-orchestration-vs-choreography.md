---
title: "Saga: Orchestration vs Choreography"
type: architecture
tags: [saga, comparison]
sources: ["raw/saga-choreography-design.md", "raw/saga-orchestration-design.md"]
related: ["[[saga-orchestration]]", "[[saga-choreography]]", "[[architecture/overview]]"]
created: 2026-10-27
updated: 2026-10-27
confidence: medium
---

# Saga: Orchestration vs Choreography

| Dimension | [[saga-orchestration]] | [[saga-choreography]] |
|---|---|---|
| Workflow definition | one `SagaDefinition` file | emergent from per-service rules |
| New infra | `saga-orchestrator` + its Postgres | none (optional `saga-watchdog`) |
| Coupling | star | chain |
| "Where is saga X?" | `GET /sagas/{id}` | build a projector or query each service |
| Add/reorder a step | edit orchestrator only | edit every affected service |
| Happy-path latency | +1 round-trip per step | minimal |
| Stall detection | built in | needs watchdog / deadline events |
| Best for | many steps, branching, operator control | few steps, stable, linear |

## Shared mechanics
Kafka transport, [[outbox-pattern]], Redis idempotency keys, idempotent always-succeeding compensations, `sagaId` correlation.

## Open question
Neither source records which variant the team chose. When one is chosen, record it as `ADR-001-...` in `decisions/` (see `TECHNICAL_DECISION_FRAMEWORK.md` for the intended decision-record format).
