# Flight Aggregation & Ticketing System

> System design for a flight search platform similar to Kayak, Google Flights, or Booking.com.
> Focuses on low latency, high consistency, and scalability.

---

## Key Requirements

| Requirement | Detail |
|---|---|
| Partner airlines | 5 |
| Hub cities | ~100 globally |
| Flights per airline/city/day | 3–5 |
| Seats per flight | ~100 (99% occupancy) |
| Pricing updates | Every 4 hours |
| Consistency | High — transactional booking |
| Auth / Access control | Not required |

---

## Throughput Estimates

| Parameter | Value | Notes |
|---|---|---|
| Total flights/day | 5 × 100 × 3 = **1,500** | Conservative estimate |
| Total seat sales/day | 1,500 × 100 = **150,000** | Near full occupancy |
| Searches per purchase | **10** | Users browse before buying |
| Search queries/day | 150,000 × 10 = **1,500,000** | High search volume |
| Seconds/day | **86,400** | — |
| Queries per second | 1,500,000 / 86,400 ≈ **~17 qps** | Manageable with vertical scaling |
| Queries/month | **~222.5 million** | Extrapolated from daily volume |

---

## Core Insights

- **Search UX** — Results sorted lowest → highest price (price-elastic demand)
- **Consistency** — Flight availability must be highly consistent to prevent double bookings
- **Pricing** — Updates every ~4 hours (not real-time), enabling safe caching windows
- **Scale** — ~17 qps is manageable with a vertically scaled SQL database
- **Caching** — Popular routes cached by user metadata + geolocation; LRU eviction policy
- **Personalization** — Recent search history informs cache priorities for common routes

---

## Architecture

```
Client
  └── Application Layer
        ├── Cache Layer (Redis / Memcached)
        │     └── Replicator Service  ──► Regional Caches
        ├── Flight Data Exchange (Airline APIs)
        ├── Database Layer (SQL)
        └── User Service (metadata + geospatial)
```

| Component | Description |
|---|---|
| **Client** | Frontend — user inputs search queries |
| **Application Layer** | Business logic, request handling, cache interaction |
| **Cache Layer** | Fast-access store for popular routes (Redis / Memcached) |
| **Flight Data Exchange** | External airline APIs for flights and pricing |
| **Database Layer** | SQL DB — flight schedules, prices, user metadata |
| **User Service** | Tracks geospatial info and search history |
| **Replicator Service** | Syncs cache across regions via pub/sub |

---

## Data Flow

1. User submits a search (departure, arrival, date)
2. App layer checks **cache** using user geolocation + popular routes
3. **Cache hit** → return results immediately (low latency)
4. **Cache miss** → query SQL DB, return results, update cache if query is common
5. User metadata tracked to identify popular routes and improve cache hit rate
6. Cache synced across regions via **pub/sub**; LRU eviction removes stale entries

---

## Example SQL Query

```sql
SELECT *
FROM flights
WHERE latitude  = :lat
  AND longitude = :lon
  AND date BETWEEN :date_from AND :date_to
ORDER BY price ASC
LIMIT 100;
```

> Cache is checked before this query executes to avoid costly DB round-trips.

---

## Conclusions

- SQL with **vertical scaling** is sufficient at ~17 qps — no NoSQL needed initially
- **Caching** (geospatial + personalized) is the primary latency optimization
- Prices update every 4 hours → cache TTL can match, simplifying consistency
- Architecture mirrors a **stock exchange analogy** — data sourced externally, updated periodically
- Further bottleneck analysis and **horizontal scaling** warranted if traffic grows significantly

---

## Key Takeaways

- Highly consistent flight availability is critical — no double bookings
- Cache popular + personalized routes to minimize DB load
- Regional cache sync ensures low latency worldwide
- Price variability allows hours-long caching without real-time constraints
- Design balances **transactional integrity** with **performance** in a price-elastic market