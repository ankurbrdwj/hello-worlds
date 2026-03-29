# spring-postgres

Spring Boot + PostgreSQL project used as a personal SQL study notebook.

## SQL Chapter Files

All chapters live in `src/main/resources/static/`. Each chapter covers one query pattern — a header block describes the pattern, then 10 numbered examples follow.

| File | Pattern |
|------|---------|
| chapter1.sql | Filter early — "Give me all X but only if Y" |
| chapter2.sql | EXISTS / NOT EXISTS — set membership tests |
| chapter3.sql | HAVING — filter after aggregation |
| chapter4.sql | Pre-aggregating — avoid join explosion by pre-agg before joining |
| chapter5.sql | LeetCode Hard 185 — Department Top Three Salaries (DENSE_RANK + correlated subquery) |
| chapter6.sql | Conditional aggregation — SUM(CASE WHEN), funnels, pivots |
| chapter7.sql | Rates, shares, and NULL-safe division — NULLIF, CTR, share of total, period-over-period |
| chapter8.sql | Ranking — ROW_NUMBER / RANK / DENSE_RANK, top-N per group, nth highest, dedup |
| chapter9.sql | Rolling metrics — ROWS/RANGE frames, date spine, cumulative, LAG, rolling min/max |
| chapter10.sql | Previous vs current — LAG/LEAD, WoW/MoM/YoY, state changes, consecutive rows |
| chapter11.sql | Self-joins — employee/manager, pairs, before/after events, recursive hierarchy |

## Seed File

`src/main/resources/static/seed.sql` — run this once to create and populate every table used across all chapters. Includes all core business tables, analytics tables, and LeetCode tables with enough rows to make every query return meaningful results.

```bash
psql -U postgres -d your_db -f src/main/resources/static/seed.sql
```

## Chapter Format Convention

Chapters 4–7 follow this header format (plain text, no SQL comment markers on the first lines):

```
<Pattern Name>
You'll see: "<signal phrases>"

What to do when you see it: <one-sentence rule>

SQL

<template query>

Tip
<key decision to make at the whiteboard>

-- ===...===
-- 1. First example
-- ===...===
...
```

Chapters 1–3 start directly with numbered `-- 1. ...` examples (no header block).

When adding a new chapter, follow the chapter4–7 format.

## Running Locally

```bash
docker-compose up -d   # starts PostgreSQL
./gradlew bootRun      # starts Spring Boot app
```