# Sharding Postgres — What to Learn, When to Do It, What It Costs

A study guide for the spring-postgres module: what sharding actually is, the decision
framework for when it's worth doing, what a developer needs to relearn about writing
queries once data is split across shards, and where CAP theorem does (and doesn't) fit in.

---

## 1. What "sharding" means here

**Sharding** = splitting one logical table (or database) horizontally across multiple
physical Postgres instances, by row, using a **shard key** (e.g. `user_id`, `tenant_id`).
Each shard holds a disjoint subset of rows and is a complete, independent Postgres
database — not just a table partition on the same server.

This is different from (and often confused with) two things that solve smaller problems:

| Technique | What it splits | Still one DB? | Solves |
|---|---|---|---|
| **Table partitioning** (`PARTITION BY RANGE/HASH/LIST`) | Rows within one table | Yes — one Postgres instance | Query pruning, faster vacuum, easier data lifecycle (drop old partitions) |
| **Read replicas** | Read traffic | One primary, N read-only copies | Read scaling, not write scaling |
| **Sharding** | Rows across multiple independent databases | No — N separate primaries | Write scaling, storage scaling beyond one machine |

Most teams should exhaust partitioning + read replicas + connection pooling + better
indexing *before* sharding. Sharding is the last resort because of what Section 4 covers —
it is expensive to build and permanently more expensive to run.

---

## 2. Purpose — why shard at all

Sharding exists to solve exactly one class of problem: **a single Postgres primary can no
longer take the write load or hold the dataset**, and vertical scaling (bigger machine)
has hit a ceiling or a cost wall. Concretely:

- **Write throughput** — one primary has a hard ceiling on WAL write rate / commit latency.
  You cannot add more primaries to one logical database without sharding.
- **Storage size** — table + index size exceeds what fits comfortably in RAM/cache on the
  biggest instance you're willing to pay for; vacuum and index maintenance start taking
  hours.
- **Blast radius / isolation** — a noisy tenant (huge customer, batch job) degrading
  performance for every other tenant on a shared primary. Sharding by `tenant_id` gives
  hard isolation.
- **Regulatory/data-residency** — EU customer data must physically live in an EU region.
  Sharding by region is sometimes the *only* way to satisfy this, not just a performance
  choice.

If your actual problem is "reads are slow," sharding is almost always the wrong tool —
add a read replica, an index, or a cache first.

---

## 3. Cost-benefit tradeoffs

| Benefit | Cost |
|---|---|
| Write throughput scales roughly linearly with shard count | Every cross-shard query becomes a fan-out + application-side merge |
| Dataset size no longer bounded by one machine | No native cross-shard transactions or foreign keys — you build your own consistency |
| Blast radius isolation (one tenant's load can't starve another) | N× the operational surface: N backups, N failover setups, N sets of metrics/alerts |
| Smaller per-shard indexes → cheaper vacuum, faster index scans | Schema migrations must run against every shard, and must be coordinated |
| Can co-locate a tenant's data for data-residency compliance | Rebalancing (splitting a hot shard) is a genuinely hard, risky online-migration problem |
| | Uniqueness constraints only hold *within* a shard, not globally, unless the shard key is part of the key |
| | Local dev/test environments get harder to represent faithfully |

The honest framing: sharding trades a **query/consistency-model tax you pay forever**
for **write/storage headroom**. It is not free scaling — it converts a capacity problem
into a permanent complexity problem. Only take that trade when the capacity problem is
real and imminent, not anticipated three years out.

---

## 4. When to actually make the call

Use this as a gate, not a vibe check. Sharding is justified when **all** of these are true,
not just one:

1. **You've already exhausted cheaper scaling.** Read replicas are in place, queries are
   indexed, hot tables are partitioned, connection pooling (pgbouncer) is in place, and
   the bottleneck is still write throughput or dataset size — not a slow query you haven't
   fixed yet.
2. **The bottleneck is on the primary, specifically.** `pg_stat_activity` and WAL metrics
   show you're commit/IO-bound on writes, or `pg_database_size` + working-set-vs-RAM shows
   you're outgrowing cache, not just disk.
3. **There's a shard key that's actually clean.** A column present on (almost) every hot
   query's `WHERE` clause, with no natural need to join or aggregate across its values in
   the common path (e.g. `tenant_id`, `user_id`, `account_id`). If your hottest queries
   need to join or aggregate across the candidate key, sharding on it will make those
   queries slower, not faster — pick a different key or don't shard.
4. **You can afford the ongoing operational cost.** N shards means N times the on-call
   surface. If a two-person team can barely operate one Postgres instance well, five
   shards will not go well.
5. **The growth curve says this doesn't wait.** If you're not within ~6-12 months of
   hitting the ceiling, spend the engineering time elsewhere first — sharding done early
   is pure premature-optimization tax, and the shard key you pick before you have real
   traffic patterns is frequently the wrong one.

If any of these is false, the right move is usually: partition the table, add a replica,
tune the query, or just buy a bigger box. Vertical scaling on modern cloud hardware (tens
of TB of RAM, tens of thousands of IOPS) covers a lot more ground than people assume
before sharding is genuinely required.

---

## 5. Does CAP theorem "advise" sharding?

No — **CAP theorem doesn't tell you whether to shard, it tells you what you give up once
you have a distributed system, which sharding turns you into.**

CAP says: under a network **P**artition between nodes, you must choose between
**C**onsistency (every node sees the same data) and **A**vailability (every node keeps
serving). A single-primary Postgres instance is not subject to CAP at all — there's only
one node holding the truth, so there's nothing to partition between. The moment you shard
across multiple independent Postgres instances, you *become* a distributed system, and
CAP-style tradeoffs now apply to anything that spans shards:

- **Cross-shard writes** (e.g. transferring a balance between two accounts on different
  shards) have no native two-phase commit in vanilla Postgres. You either accept eventual
  consistency (sagas, outbox pattern — see `outbox-pattern` module in this repo) or bolt
  on a distributed transaction coordinator, which costs latency and availability.
- **Cross-shard reads** (e.g. "top 10 users across all tenants") can't be a single
  consistent snapshot without a fan-out-and-merge that itself isn't atomic across shards.
- **Global uniqueness / global secondary indexes** (e.g. "email must be unique across all
  users, but you sharded by `tenant_id`") don't exist for free — you need a separate
  lookup service/table or you accept it's enforced at the application layer.

So the causal arrow is the opposite of the question: CAP doesn't recommend sharding.
**Sharding is a decision you make for throughput/storage reasons (Section 2), and CAP is
the bill that arrives afterward** — it tells you which correctness guarantees you now have
to explicitly design for instead of getting for free from a single Postgres instance's
ACID transactions.

---

## 6. What a new developer needs to learn to write queries against a sharded Postgres

This is the part that actually changes day-to-day work. Ranked roughly by how often it
bites people:

### 6.1 Always filter (or route) by the shard key
Every query needs the shard key in `WHERE`, or an explicit target shard, before it's
issued — otherwise the routing layer (or your own connection-picking code) doesn't know
which of the N databases to talk to.

```sql
-- Fine on a single, unsharded Postgres:
SELECT * FROM orders WHERE created_at > now() - interval '1 day';

-- On a Postgres sharded by tenant_id, this query is ambiguous — which shard?
-- You must know the tenant up front:
SELECT * FROM orders
WHERE tenant_id = :tenant_id           -- shard key: picks the connection/shard
  AND created_at > now() - interval '1 day';
```

### 6.2 There is no cross-shard JOIN
A `JOIN` only works against tables that live on the *same* physical Postgres instance.
If two tables that need to be joined can end up on different shards (e.g. `orders` sharded
by `tenant_id`, `products` shared/global), the join has to be restructured:

- Co-locate: shard both tables by the same key so related rows always land on the same
  shard (`orders` and `order_items` both by `tenant_id` — safe).
- Or split it into two queries and join in the application: fetch from shard A, fetch from
  shard B (or a shared "reference data" table replicated everywhere), stitch in code.
- Reference/lookup tables that rarely change (currencies, plan tiers) are usually kept
  **unsharded and replicated to every shard**, specifically so they can still be joined
  locally on each shard.

### 6.3 Fan-out queries for anything cross-tenant
"Give me the top 10 orders across all tenants" has no single-shard answer. You issue the
query to every shard in parallel, then merge/re-sort in the application:

```
for shard in all_shards:
    results[shard] = shard.query("SELECT * FROM orders ORDER BY total DESC LIMIT 10")
merge_and_resort(results.values())[:10]
```

This is N round trips instead of 1, it's slower, and it doesn't compose with `LIMIT`
cheaply (you must over-fetch `LIMIT` rows *per shard*, not just once, to be correct after
the merge). New devs consistently underestimate how much of the "just add a dashboard
query" work turns into fan-out code once sharded.

### 6.4 No global auto-increment, no global uniqueness
`SERIAL`/`BIGSERIAL` primary keys are per-shard — two shards will both mint id `1`. You
need either:
- A composite key that includes the shard key (`(tenant_id, id)`), or
- A globally-unique ID scheme independent of any single shard's sequence — e.g. UUIDs, or
  a Snowflake-style ID (timestamp + shard-id + sequence) generated by the application.

Similarly, `UNIQUE (email)` only enforces uniqueness *within* a shard. A user signing up
with an email that exists on a different shard won't be caught by the database — you need
a separate global lookup (often a small unsharded "directory" table: `email -> shard_id`).

### 6.5 Transactions don't span shards
A `BEGIN ... COMMIT` block is scoped to one Postgres connection, i.e. one shard. Moving
money between two accounts on different shards can't be one ACID transaction — this is
exactly the CAP tradeoff from Section 5. The standard pattern is the **outbox/saga**
approach already demonstrated in this repo's `outbox-pattern` module: write locally with a
durable "intent" row, then use an async process to apply the compensating action on the
other shard, with retries and a reconciliation job for the cases that fail partway.

### 6.6 Schema migrations run N times, and must be compatible mid-rollout
`ALTER TABLE` has to run against every shard. During the rollout window, some shards have
the new column and some don't — application code has to tolerate both, the same
discipline as a zero-downtime migration on a single DB, just multiplied by N and now with
partial-completion risk (migration succeeds on 8 of 10 shards, then fails).

### 6.7 Rebalancing / resplitting a hot shard
Shard keys that seemed evenly distributed at launch stop being even as usage patterns
diverge (one tenant grows 100x). Moving a subset of rows from an overloaded shard to a new
one, with the application still serving live traffic, is one of the hardest operational
tasks in this space — plan for it (consistent hashing, or a routing table that maps key
ranges to shards so ranges can move without changing the hashing scheme) *before* you need
it, not after a shard is already on fire.

---

## 7. Query performance — the honest picture

**Gets better:**
- Each shard has a smaller table/index → hotter cache hit ratio, faster index scans,
  cheaper vacuum/autovacuum cycles.
- Writes parallelize across shards — N shards can absorb roughly N× the write throughput
  of one primary (network/app-layer overhead aside).
- Queries that are naturally scoped to one shard key (the common case in a well-chosen
  design, e.g. "this tenant's dashboard") see **no regression** and often an improvement,
  since they're now hitting a smaller, hotter table.

**Gets worse:**
- Anything cross-shard (Section 6.3) trades one query for N queries plus an
  application-side merge — higher latency, more app-layer complexity, and `ORDER BY ...
  LIMIT` semantics get subtle (see over-fetch note above).
- Aggregations across shards (`COUNT(*)`, `SUM(...)` over all tenants) must be computed
  per-shard and combined — trivial for `SUM`/`COUNT`, genuinely hard for things like exact
  `COUNT(DISTINCT ...)` or percentiles, which don't merge cleanly across partial results.
- Query planning/tooling (`EXPLAIN`, `pg_stat_statements`) is now per-shard — there's no
  single query plan to look at for a fan-out query; you have to reason about N plans and
  the merge step together.

---

## 8. Maintainability

This is the cost that's easy to underweight when the decision is being made under
performance pressure, and expensive for years afterward:

- **Operational multiplication.** Backups, point-in-time recovery, failover, monitoring,
  alerting, capacity planning — all of it now happens N times, and N isn't fixed, it grows.
- **Tooling either needs to be shard-aware, or you build a routing layer.** Options in the
  Postgres ecosystem: [Citus](https://www.citusdata.com/) (turns Postgres into a
  distributed database with a coordinator that transparently routes/fans-out queries —
  by far the least custom-code-heavy path if you're staying in Postgres), or fully
  application-level sharding (your own connection-picking logic, as sketched in Section 6
  — more control, much more code to own).
- **Local dev/test drift.** It's tempting to run a single unsharded Postgres locally and
  "sharding" only in staging/prod — this reliably hides bugs (missing shard-key filters,
  accidental cross-shard joins) until they hit production. Budget for either a
  multi-shard local setup (docker-compose with N Postgres containers) or a lightweight
  in-process router that can run in "1 shard" mode locally but exercises the same code
  path as N-shard prod.
- **Onboarding cost.** Every new developer has to learn Sections 6.1–6.7 before they can
  safely write a query — that's real ramp-up time that doesn't exist on a single Postgres
  instance, and it recurs with every hire.

---

## 9. Minimal decision checklist

Before sharding this (or any) Postgres-backed service, confirm:

- [ ] Read replicas, indexing, and partitioning are already in place and insufficient
- [ ] The bottleneck is measurably on the primary (write throughput or dataset size), not
      an unfixed slow query
- [ ] A shard key exists that covers the vast majority of hot-path queries
- [ ] A plan exists for cross-shard needs: reference-data replication, fan-out queries,
      global uniqueness/ID generation
- [ ] A plan exists for cross-shard writes: saga/outbox pattern, not distributed
      transactions
- [ ] The team can operate N databases, not one — backups, failover, monitoring, on-call
- [ ] A rebalancing strategy is chosen before day one (consistent hashing or a movable
      range-to-shard map), not designed reactively once a shard is hot
- [ ] Local/test environments can exercise real multi-shard behavior, not just prod

If any box is unchecked, that's the next thing to fix — not a reason to shard anyway and
hope it works out.