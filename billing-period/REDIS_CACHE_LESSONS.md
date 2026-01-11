i# Redis Caching & Cache Invalidation Problems - Lessons Learned

## What We Built

A production-ready Spring Boot application with:
- ✅ Redis cache with Docker Compose
- ✅ Two REST endpoints with caching
- ✅ Different TTL strategies for different data types
- ✅ Performance improvement: **3.6x faster** with caching

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────┐
│   Client    │────▶│  Controller  │────▶│  Service   │
└─────────────┘     └──────────────┘     └────────────┘
                            │                    │
                            │                    ▼
                            │            ┌────────────────┐
                            │            │  @Cacheable    │
                            │            │  checks Redis  │
                            │            └────────────────┘
                            ▼                    │
                    ┌──────────────┐            │
                    │    Redis     │◀───────────┘
                    │   Cache DB   │
                    └──────────────┘
```

## The 4 Famous Cache Invalidation Problems

### Problem 1: Stale Data
**"There are only two hard things in Computer Science: cache invalidation and naming things." - Phil Karlton**

**What**: Cache holds old data after source data changes

**Example**:
```
1. User requests billing periods → cached in Redis
2. Business rules change (e.g., periods now start on Mondays)
3. Cache still returns old periods (Saturdays)
4. Users see incorrect data! 💥
```

**Solutions**:
- ✅ TTL (Time To Live): Auto-expire after X minutes
- ✅ Manual invalidation: `@CacheEvict` when data changes
- ✅ Event-driven invalidation: Redis Pub/Sub to notify services
- ✅ Cache warming: Pre-populate cache on startup

**In Our Code**: See `CacheConfig.java`
```java
.entryTtl(Duration.ofHours(1))  // Billing periods change rarely
.entryTtl(Duration.ofHours(24)) // Year data changes very rarely
```

### Problem 2: Cache Stampede (Thundering Herd)

**What**: Cache expires → 1000 concurrent requests hit database simultaneously

**Scenario**:
```
11:00 AM: Cache expires (TTL reached)
11:00:01 AM: 1000 users request same data
11:00:01 AM: All 1000 see cache miss
11:00:01 AM: All 1000 hit database at once
11:00:02 AM: Database overwhelmed! 🔥💥
```

**Solutions**:
- ✅ Staggered TTL: Add random jitter (e.g., TTL = 60min ± 5min)
- ✅ Cache locking: First request locks, others wait
- ✅ Refresh-ahead: Refresh cache before expiry
- ✅ Request coalescing: Merge duplicate requests

**Production Code**:
```java
// Bad: Fixed TTL
.entryTtl(Duration.ofMinutes(60))

// Good: Staggered TTL
.entryTtl(Duration.ofMinutes(60 + ThreadLocalRandom.current().nextInt(10)))
```

### Problem 3: Cache Consistency

**What**: Related caches get out of sync

**Example in Our Code**:
- `billingPeriods::2019-01-15` → Single period
- `yearPeriods::2019` → All periods for year

**Problem**:
```
1. Cache individual period for Jan 15, 2019
2. Business rules change
3. Invalidate yearPeriods::2019
4. BUT billingPeriods::2019-01-15 still cached!
5. Inconsistent data! 💥
```

**Solutions**:
- ✅ Invalidate related caches together
- ✅ Use cache tags/groups
- ✅ Event-driven invalidation
- ✅ Consider if you need multiple cache levels

**Code Example**:
```java
@CacheEvict(value = {"billingPeriods", "yearPeriods"}, allEntries = true)
public void updateBusinessRules() {
    // Clear ALL related caches
}
```

### Problem 4: Memory Pressure

**What**: Cache grows too large → Redis OOM (Out Of Memory)

**Scenario**:
```
Day 1: Cache 2019 periods (62 entries)
Day 2: Cache 2020 periods (62 entries)
...
Day 365: Cache 365 years × 62 periods = 22,630 entries
Redis: 💀 Out of memory!
```

**Solutions**:
- ✅ Set Redis `maxmemory` limit
- ✅ Configure eviction policy: `maxmemory-policy allkeys-lru`
- ✅ Monitor cache size
- ✅ Use appropriate TTL
- ✅ Cache only hot data

**Redis Configuration**:
```yaml
# compose.yaml
command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

## Cache Invalidation Strategies

### Strategy 1: Time-Based (TTL)
```java
@Cacheable(value = "billingPeriods")
.entryTtl(Duration.ofHours(1))
```
✅ Simple, automatic
❌ May serve stale data
🎯 Use for: Rarely changing data

### Strategy 2: Manual Invalidation
```java
@CacheEvict(value = "billingPeriods", key = "#date.toString()")
public void updatePeriod(LocalDate date) {
    // Update and clear cache
}
```
✅ Immediate invalidation
❌ Must track all cache dependencies
🎯 Use for: Critical, frequently updated data

### Strategy 3: Event-Driven
```java
@EventListener
public void onBusinessRulesChanged(BusinessRulesChangedEvent event) {
    cacheManager.getCache("billingPeriods").clear();
}
```
✅ Scales across services
❌ Complex to implement
🎯 Use for: Microservices, distributed systems

### Strategy 4: Write-Through
```java
@CachePut(value = "billingPeriods", key = "#result.periodId")
public BillingPeriod savePeriod(BillingPeriod period) {
    return repository.save(period);
}
```
✅ Cache always fresh
❌ Write latency
🎯 Use for: Write-heavy workloads

## Performance Results

| Scenario | Without Cache | With Redis Cache | Speedup |
|----------|--------------|------------------|---------|
| GET /periods?year=2019 (first) | 66ms | - | - |
| GET /periods?year=2019 (cached) | - | 18ms | **3.6x** |
| GET /periods?year=2020 (first) | 66ms | - | - |
| GET /periods?year=2020 (cached) | - | 18ms | **3.6x** |

## Redis Commands for Debugging

```bash
# See all cached keys
docker exec billing-period-redis-1 redis-cli KEYS "*"

# Get cached value
docker exec billing-period-redis-1 redis-cli GET "yearPeriods::2019"

# Check TTL (time to live)
docker exec billing-period-redis-1 redis-cli TTL "yearPeriods::2019"

# Clear specific cache
docker exec billing-period-redis-1 redis-cli DEL "yearPeriods::2019"

# Clear all caches
docker exec billing-period-redis-1 redis-cli FLUSHALL

# Monitor cache hits/misses in real-time
docker exec billing-period-redis-1 redis-cli MONITOR
```

## Key Files

- `compose.yaml` - Redis container configuration
- `CacheConfig.java` - Redis cache configuration with TTL strategies
- `BillingPeriodService.java` - @Cacheable annotations
- `application.properties` - Redis connection settings

## Testing the Cache

```bash
# Start Redis
docker compose up -d

# Start application
./gradlew bootRun

# Test cache miss (slow)
curl "http://localhost:8080/periods?year=2019"

# Test cache hit (fast!)
curl "http://localhost:8080/periods?year=2019"

# Check what's in Redis
docker exec billing-period-redis-1 redis-cli KEYS "*"
```

## Production Checklist

- [ ] Set appropriate TTL for each cache
- [ ] Configure Redis maxmemory and eviction policy
- [ ] Monitor cache hit ratio
- [ ] Implement cache warming for critical data
- [ ] Add cache invalidation hooks
- [ ] Test cache stampede scenarios
- [ ] Document cache dependencies
- [ ] Set up Redis persistence (AOF or RDB)
- [ ] Configure Redis replication for HA
- [ ] Monitor Redis memory usage

## Common Pitfalls

1. **Caching everything** → Memory pressure
2. **TTL too long** → Stale data
3. **TTL too short** → Cache thrashing
4. **No monitoring** → Can't detect issues
5. **Forgetting to invalidate** → Stale data
6. **Complex cache keys** → Hard to invalidate
7. **No fallback** → Redis down = app down

## Further Reading

- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Redis Caching Best Practices](https://redis.io/docs/manual/patterns/)
- [Cache Stampede Problem](https://en.wikipedia.org/wiki/Cache_stampede)
- [The Two Hard Things](https://martinfowler.com/bliki/TwoHardThings.html)

---

**Remember**: "Premature optimization is the root of all evil" - Donald Knuth

Only add caching when you have a proven performance problem! 🚀