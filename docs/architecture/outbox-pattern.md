---
title: "Transactional Outbox Pattern"
type: architecture
tags: [outbox, kafka, postgres, reliability]
sources: ["raw/outbox-pattern-readme.md"]
related: ["[[saga-orchestration]]", "[[architecture/overview]]"]
created: 2026-10-27
updated: 2026-10-27
confidence: high
---

# Transactional Outbox Pattern

Implemented in the `outbox-pattern` module (Spring Boot, Postgres, Docker).

## Problem: dual write
Saving business data and publishing an event are two separate systems; one can succeed while the other fails, leaving data and events inconsistent.

## Solution
1. **One DB transaction** writes the business row (`orders`) *and* an `outbox_events` row. Both commit or both roll back.
2. **A background process** polls `outbox_events` where `status='PENDING'`, publishes each to the broker (Kafka), then marks it `PROCESSED`.

## In this repo
- `OrderService.createOrder` is `@Transactional` and saves the order and the `OutboxEvent` together.
- `OutboxProcessor` is the scheduled relay that publishes pending events.
- Delivery is **at-least-once**: consumers must be idempotent.

## Reused by
- [[saga-orchestration]] copies this relay mechanism for its `saga_outbox` and for participant replies.
