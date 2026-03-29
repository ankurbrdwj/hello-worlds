# Kafka Fundamentals & Spring Kafka

---

## Core Concepts

### Topics

A **topic** is a named, ordered log of records — the fundamental unit of organisation in Kafka.
Producers write to a topic; consumers read from it. Topics are append-only and immutable.

```
Topic: crypto-trades
 [trade-1] [trade-2] [trade-3] [trade-4] ...
```

### Partitions

Every topic is split into **partitions** — independent, ordered sub-logs stored on disk.

- Each partition lives on one broker (with replicas on others).
- Messages within a partition are strictly ordered.
- Ordering across partitions is **not** guaranteed.
- Partition count controls maximum parallelism.

```
Topic: crypto-trades  (3 partitions)

Partition 0: [BTC-1] [ETH-3] [BTC-5]
Partition 1: [ETH-2] [BTC-4]
Partition 2: [SOL-1] [SOL-2] [ETH-4]
```

**Partition key** determines which partition a message lands on.
Use `symbol` (BTC, ETH) or `accountId` — events for the same key always go to the same partition,
guaranteeing order per key.

### Consumer Groups

A **consumer group** is a pool of consumers that collectively read a topic.
Each partition is assigned to exactly one consumer in the group at any time.

```
Topic: crypto-trades (6 partitions)
Consumer group: trade-processor (3 instances)

Consumer-1 → Partition 0, 1
Consumer-2 → Partition 2, 3
Consumer-3 → Partition 4, 5
```

Rules:
- Adding consumers up to the partition count increases parallelism.
- Consumers beyond the partition count sit idle.
- Multiple independent consumer groups each read the full topic (no sharing of offsets).

### Offsets

An **offset** is a monotonically increasing integer that uniquely identifies a message within a partition.
Each consumer group tracks its own offset per partition — this is what allows independent consumption
and replay.

```
Partition 0:  [offset 0] [offset 1] [offset 2] [offset 3] ...
                                        ^
                              consumer-group-A committed here
```

Committing an offset tells Kafka "everything up to here is processed."
If a consumer restarts, it resumes from the last committed offset.

---

## Delivery Guarantees

### At-Least-Once (default, recommended)

- Kafka delivers every message at least once.
- Duplicates are possible (consumer crashes after processing but before committing the offset).
- **Consumers must be idempotent**: processing the same message twice produces the same result.

**Financial example:** Instead of `INCREMENT balance BY 50`, store `SET balance TO 1050 WHERE version = 42`.
The second execution is a no-op because the version no longer matches.

```java
// Idempotent: safe to apply twice
@KafkaListener(topics = "trade-settlements")
public void settle(TradeEvent event) {
    if (!settlementRepo.existsByTradeId(event.getTradeId())) {
        settlementRepo.save(Settlement.from(event));
    }
}
```

### At-Most-Once

- Offset is committed before processing.
- A consumer crash means the message is lost — never replayed.
- Acceptable only for non-critical analytics / metrics where a few lost events are tolerable.
- **Never use in financial contexts.**

### Exactly-Once (EOS)

- Each message is processed and its effect committed exactly once.
- Kafka achieves this via **idempotent producers** + **transactions** spanning produce + consume + produce.
- Significant throughput cost; only within the Kafka ecosystem (producer → Kafka → Kafka Streams → Kafka).
- Does **not** cover external side effects (DB writes, REST calls).

**Practical advice:** Use **at-least-once + idempotent consumers** in production.
Promise exactly-once only when you can defend the transaction boundary.

---

## Kafka Consumer API vs Kafka Streams

### Kafka Consumer API (Spring `@KafkaListener`)

Low-level. You control the poll loop, offset commits, and processing logic manually.

```java
@KafkaListener(topics = "crypto-trades", groupId = "trade-processor")
public void consume(TradeEvent event) {
    enrichmentService.enrich(event);
    tradeRepository.save(event);
}
```

**Use when:**
- Simple consume → process → store pipeline.
- Side effects go outside Kafka (database, REST API).
- You need full control over error handling and retries.

### Kafka Streams

A JVM library for **stateful stream processing** entirely within Kafka.
Reads from input topics, transforms/aggregates, writes to output topics.

```java
StreamsBuilder builder = new StreamsBuilder();

builder.stream("crypto-trades")                        // source
       .filter((key, trade) -> trade.getVolume() > 0)
       .mapValues(EnrichmentService::enrich)           // stateless transform
       .groupByKey()
       .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
       .count()                                        // stateful aggregate
       .toStream()
       .to("trade-volume-per-minute");                 // sink
```

**Use when:**
- Joins, windowed aggregations, running totals across a stream.
- Output stays in Kafka (another topic), not a direct DB write.
- You want Kafka-managed exactly-once for the stream processing step.

| | Consumer API | Kafka Streams |
|---|---|---|
| Complexity | Low | Medium–High |
| State management | Manual (your DB) | Built-in (RocksDB) |
| Joins / windowing | Manual | Native |
| Exactly-once | Hard | Supported within Kafka |
| External side effects | Natural | Awkward |

---

## Dead Letter Topics (DLT) for Financial Error Handling

A **Dead Letter Topic** is where messages go when they cannot be processed after N retries.
Without one, a single corrupt or unprocessable message blocks the partition forever — a *poison pill*.

### Spring Kafka configuration

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<?, ?> template) {
    // Exponential backoff: 1s, 2s, 4s — max 3 attempts
    ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
    backoff.setInitialInterval(1_000);
    backoff.setMultiplier(2);

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
        (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
    );

    return new DefaultErrorHandler(recoverer, backoff);
}
```

Kafka automatically routes failed messages to `crypto-trades.DLT` after 3 attempts,
with the original topic/partition/offset and exception stored as headers.

### Financial DLT strategy

```
crypto-trades
      │
      ├── success → enrichment-topic
      │
      └── 3 failures → crypto-trades.DLT
                              │
                        [alert ops team]
                        [audit log entry: FAILED]
                        [manual review / replay tool]
```

**Key rules for financial systems:**
- Always write a DLT entry to the audit log (compliance requires it).
- Never silently discard — regulators expect full traceability.
- Provide a replay tool: fix the root cause, then re-publish from DLT to the original topic.
- Set retention on DLT to at least 30 days (or per your compliance policy).

---

## Crypto Trade Topology: End-to-End Design

```
┌─────────────────────────────────────────────────────────────────┐
│                    CRYPTO TRADE TOPOLOGY                         │
│                                                                   │
│  Exchange Feed                                                     │
│      │                                                            │
│      ▼                                                            │
│  [crypto-trades]          Raw trade events                        │
│  partition key: symbol                                            │
│      │                                                            │
│      ▼                                                            │
│  EnrichmentConsumer       Adds: market data, user profile,        │
│  group: enricher          FX rates, compliance flags              │
│      │                                                            │
│      ├──── success ──► [enriched-trades]                         │
│      └──── failure ──► [crypto-trades.DLT]                       │
│                                                                   │
│  [enriched-trades]                                                │
│      │                                                            │
│      ▼                                                            │
│  PersistenceConsumer      Idempotent write to trade DB            │
│  group: db-writer         (dedup by tradeId)                      │
│      │                                                            │
│      ├──── success ──► [trade-audit-events]                      │
│      └──── failure ──► [enriched-trades.DLT]                     │
│                                                                   │
│  [trade-audit-events]                                             │
│      │                                                            │
│      ▼                                                            │
│  AuditLogConsumer         Append-only write to audit store        │
│  group: auditor           (immutable compliance log)              │
│      │                                                            │
│      └──── failure ──► [trade-audit-events.DLT]                  │
│                              + alert ops immediately              │
└─────────────────────────────────────────────────────────────────┘
```

### Topic configuration

| Topic | Partitions | Retention | Key |
|---|---|---|---|
| `crypto-trades` | 12 | 7 days | `symbol` |
| `enriched-trades` | 12 | 7 days | `symbol` |
| `trade-audit-events` | 12 | 90 days | `tradeId` |
| `*.DLT` | match source | 30 days | inherited |

### Enrichment stage

```java
@KafkaListener(topics = "crypto-trades", groupId = "enricher")
public void enrich(TradeEvent raw, Acknowledgment ack) {
    EnrichedTrade enriched = EnrichedTrade.builder()
        .trade(raw)
        .marketPrice(priceService.getPrice(raw.getSymbol()))
        .userProfile(userService.getProfile(raw.getUserId()))
        .fxRate(fxService.getRate(raw.getCurrency(), "USD"))
        .complianceFlag(complianceService.check(raw))
        .build();

    kafkaTemplate.send("enriched-trades", raw.getSymbol(), enriched);
    ack.acknowledge();   // commit only after successful produce
}
```

### Persistence stage (idempotent DB write)

```java
@KafkaListener(topics = "enriched-trades", groupId = "db-writer")
@Transactional
public void persist(EnrichedTrade trade, Acknowledgment ack) {
    // INSERT ... ON CONFLICT (trade_id) DO NOTHING
    tradeRepository.insertIfAbsent(trade);

    kafkaTemplate.send("trade-audit-events", trade.getTradeId(),
        AuditEvent.of(trade, AuditAction.PERSISTED));

    ack.acknowledge();
}
```

### Audit log stage

The audit log is **append-only** — no updates, no deletes.
Consumers write a structured record for every action taken on every trade.

```java
@KafkaListener(topics = "trade-audit-events", groupId = "auditor")
public void audit(AuditEvent event, Acknowledgment ack) {
    auditStore.append(AuditRecord.builder()
        .tradeId(event.getTradeId())
        .action(event.getAction())
        .timestamp(Instant.now())
        .actorService(event.getSource())
        .payload(event.getPayload())
        .build());

    ack.acknowledge();
}
```

### Why three separate consumer groups?

Each stage scales independently:
- Enrichment is CPU-bound (external calls) — scale out aggressively.
- DB writes are I/O-bound — scale to match DB connection pool.
- Audit writes are lightweight — fewer instances needed.

A failure in persistence does **not** block enrichment.
A DLT entry in audit does **not** reprocess the trade.

### Delivery guarantee per stage

| Stage | Guarantee | Mechanism |
|---|---|---|
| Enrichment → enriched-trades | At-least-once | Ack after successful produce |
| DB write | At-least-once + idempotent | `ON CONFLICT DO NOTHING` on `tradeId` |
| Audit log | At-least-once | Append-only store absorbs duplicates |

---

## Partition Key Decisions

| What you want | Choose |
|---|---|
| Order all BTC trades in sequence | `symbol` |
| Order all trades for one account | `accountId` |
| Maximise even distribution (no ordering need) | `tradeId` (random UUID) |

**Hot partition warning:** partitioning by `symbol` during a BTC price spike
will hammer one partition. Monitor consumer lag per partition, not just total lag.

---

## Monitoring Checklist

- **Consumer lag** per partition — rising lag means consumers can't keep up.
- **DLT message count** — any non-zero value needs immediate investigation in finance.
- **Commit rate vs poll rate** — divergence indicates slow processing or errors.
- **Broker under-replicated partitions** — data durability risk.