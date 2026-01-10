package com.ankur.billing.period.cache;

import com.ankur.billing.period.entity.BillingPeriod;
import com.ankur.billing.period.service.BillingPeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test demonstrates caching and the infamous cache invalidation problems
 * in production systems.
 *
 * "There are only two hard things in Computer Science: cache invalidation and naming things."
 * - Phil Karlton
 */
@SpringBootTest
class CacheInvalidationTest {

    @Autowired
    private BillingPeriodService billingPeriodService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        // Clear cache before each test
        if (cacheManager.getCache("billingPeriods") != null) {
            cacheManager.getCache("billingPeriods").clear();
        }
    }

    @Test
    void demonstrateCachingBehavior() {
        // First call - cache miss, fetches from service
        BillingPeriod period1 = billingPeriodService.getPeriodForDate(LocalDate.of(2019, 1, 15));
        assertNotNull(period1);
        assertEquals("2019-3", period1.getPeriodId());

        // Second call - cache hit, should return same instance from cache
        BillingPeriod period2 = billingPeriodService.getPeriodForDate(LocalDate.of(2019, 1, 15));
        assertNotNull(period2);
        assertEquals("2019-3", period2.getPeriodId());

        // Verify data is cached
        assertNotNull(cacheManager.getCache("billingPeriods"));
    }

    @Test
    void demonstrateStaleDataProblem() {
        // PROBLEM 1: Stale Data
        // Cache stores old data, but source data has changed

        LocalDate testDate = LocalDate.of(2019, 2, 10);

        // First call populates cache
        BillingPeriod cachedPeriod = billingPeriodService.getPeriodForDate(testDate);
        assertNotNull(cachedPeriod);

        // In production: Business rules change, periods are recalculated
        // But cache still holds old data!
        // This is the #1 cache invalidation problem

        // Question: How do we know when to invalidate?
        // Question: What if multiple services share this cache?

        System.out.println("Stale data problem: Cache holds old period definitions even after business rules change!");
    }

    @Test
    void demonstrateCacheInvalidationTiming() {
        // PROBLEM 2: When to invalidate?
        // - Too early: Cache misses, performance degrades
        // - Too late: Stale data served to users
        // - Too often: Cache thrashing, defeating the purpose

        List<BillingPeriod> periods2019 = billingPeriodService.getAllPeriodsForYear(2019);
        assertNotNull(periods2019);

        // If we invalidate individual period cache, should we invalidate year cache?
        // If year changes, should we invalidate all individual periods?
        // This is cache consistency problem!

        System.out.println("Cache invalidation timing: When exactly should we clear the cache?");
    }

    @Test
    void demonstrateCacheStampedeProblem() {
        // PROBLEM 3: Cache Stampede (Thundering Herd)
        // Cache expires, multiple requests hit service simultaneously
        // All try to populate cache at once!

        // Simulate cache clear
        cacheManager.getCache("billingPeriods").clear();

        // In production: 1000 concurrent requests come in
        // All see cache miss, all query database simultaneously
        // Database gets overwhelmed!

        System.out.println("Cache stampede: Expired cache causes all requests to hit database at once!");
    }
}