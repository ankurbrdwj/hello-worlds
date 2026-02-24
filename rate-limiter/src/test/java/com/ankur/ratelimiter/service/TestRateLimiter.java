package com.ankur.ratelimiter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sliding Window Counter uses TWO counters (current + previous window)
 * instead of storing all timestamps like Sliding Window Log.
 */
public class TestRateLimiter {

    @Test
    public void testBasicRateLimiting() {
        // Create limiter: max 3 requests per 100ms window
        RateLimiter limiter = new CounterLimiterImpl(3, 100);

        // All 4 requests happen within the same 100ms window [1000-1100ms]
        assertTrue(limiter.allow("user1", 1000));  // 1st request - allow
        assertTrue(limiter.allow("user1", 1010));  // 2nd request - allow
        assertTrue(limiter.allow("user1", 1020));  // 3rd request - allow (hit limit)
        assertFalse(limiter.allow("user1", 1030)); // 4th request - reject (over limit)
    }

    @Test
    public void testWindowTransition() {
        // Create limiter: max 3 requests per 100ms window
        RateLimiter limiter = new CounterLimiterImpl(3, 100);

        // Window 0: [0ms - 100ms]
        // Add 3 requests in first window (fill to limit)
        assertTrue(limiter.allow("user2", 10));   // Window 0: count=1
        assertTrue(limiter.allow("user2", 20));   // Window 0: count=2
        assertTrue(limiter.allow("user2", 30));   // Window 0: count=3

        // Window 1: [100ms - 200ms]
        // At 110ms, we're 10ms into Window 1
        // overlap = (100 - 10) / 100 = 0.9
        // weighted BEFORE = (3 × 0.9) + 0 = 2.7 < 3 ✓ allow
        assertTrue(limiter.allow("user2", 110));  // Window 1: count=1

        // At 111ms, still in Window 1
        // overlap = (100 - 11) / 100 = 0.89
        // weighted BEFORE = (3 × 0.89) + 1 = 3.67 >= 3 ✗ reject
        assertFalse(limiter.allow("user2", 111)); // Window 1: weighted=3.67, reject

        // At 170ms, we're 70ms into Window 1
        // overlap = (100 - 70) / 100 = 0.3
        // weighted BEFORE = (3 × 0.3) + 1 = 1.9 < 3 ✓ allow
        assertTrue(limiter.allow("user2", 170));  // Window 1: count=2, weighted=1.9
    }

    @Test
    public void testMultipleUsers() {
        // Create limiter: max 2 requests per 100ms window
        RateLimiter limiter = new CounterLimiterImpl(2, 100);

        // User "alice" makes 2 requests - hits her limit
        assertTrue(limiter.allow("alice", 1000));  // alice: count=1
        assertTrue(limiter.allow("alice", 1010));  // alice: count=2
        assertFalse(limiter.allow("alice", 1020)); // alice: reject (at limit)

        // User "bob" has independent counter - can still make requests
        assertTrue(limiter.allow("bob", 1000));    // bob: count=1 (independent)
        assertTrue(limiter.allow("bob", 1010));    // bob: count=2
        assertFalse(limiter.allow("bob", 1020));   // bob: reject (at limit)
    }

    @Test
    public void testOldWindowExpires() {
        // Create limiter: max 3 requests per 100ms window
        RateLimiter limiter = new CounterLimiterImpl(3, 100);

        // Window 0: [0ms - 100ms] - Fill to limit
        assertTrue(limiter.allow("user3", 10));   // Window 0: count=1
        assertTrue(limiter.allow("user3", 20));   // Window 0: count=2
        assertTrue(limiter.allow("user3", 30));   // Window 0: count=3

        // Window 1: [100ms - 200ms] - No requests

        // Window 2: [200ms - 300ms]
        // At 250ms, previous window is Window 1 (had 0 requests)
        // Window 0 data is completely expired
        //
        // Weighted count = (0 × 0.5) + 0 = 0
        // Fresh start!
        assertTrue(limiter.allow("user3", 250));  // Window 2: count=1, weighted=1.0
        assertTrue(limiter.allow("user3", 260));  // Window 2: count=2, weighted=2.0
        assertTrue(limiter.allow("user3", 270));  // Window 2: count=3, weighted=3.0
        assertFalse(limiter.allow("user3", 280)); // Window 2: reject
    }

    @Test
    public void testMemoryCleanup() {
        // Create limiter: max 3 requests per 100ms window
        CounterLimiterImpl limiter = new CounterLimiterImpl(3, 100);

        // Add requests for multiple users
        assertTrue(limiter.allow("user1", 1000));
        assertTrue(limiter.allow("user2", 1000));
        assertTrue(limiter.allow("user3", 1000));

        // Should have 3 tracked keys
        assertTrue(limiter.getTrackedKeyCount() == 3);

        // Cleanup entries idle for more than 500ms
        // At time 2000ms, all entries (last accessed at 1000ms) are 1000ms old
        limiter.cleanup(2000, 500);

        // All entries should be removed (they're older than 500ms)
        assertTrue(limiter.getTrackedKeyCount() == 0);

        // New request should work (creates fresh entry)
        assertTrue(limiter.allow("user1", 2000));
        assertTrue(limiter.getTrackedKeyCount() == 1);
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        // Create limiter: max 10 requests per 100ms window
        CounterLimiterImpl limiter = new CounterLimiterImpl(10, 100);

        // Use a fixed timestamp for all threads
        long now = 1000;
        int numThreads = 20;
        Thread[] threads = new Thread[numThreads];

        // Track how many requests were allowed
        java.util.concurrent.atomic.AtomicInteger allowedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // Launch 20 threads, each trying to make a request for the same user
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                if (limiter.allow("user1", now)) {
                    allowedCount.incrementAndGet();
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Exactly 10 requests should be allowed (the limit), not more
        assertTrue(allowedCount.get() == 10, "Expected 10 allowed, got " + allowedCount.get());
    }
}