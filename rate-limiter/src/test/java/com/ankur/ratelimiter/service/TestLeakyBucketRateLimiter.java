package com.ankur.ratelimiter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leaky Bucket Rate Limiter:
 * - Requests queue in a bucket
 * - Bucket "leaks" (processes) at constant rate
 * - Unlike Token Bucket, does NOT allow bursts
 * - Enforces smooth, constant rate
 */
public class TestLeakyBucketRateLimiter {

    @Test
    public void testConstantRateProcessing() {
        // LeakyBucketRateLimiter(capacity, leakRateMillis)
        //
        // capacity = 3:
        //   Maximum requests that can queue in the bucket
        //
        // leakRateMillis = 100:
        //   Time to process each request (leak rate)
        //   Each request takes 100ms to "leak out"
        //
        // So: Processes 1 request every 100ms, max 3 requests queued
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(3, 100);

        // Time 1000ms: First request
        // Queue time: 0ms -> 100ms (will be processed by 1100ms)
        assertTrue(limiter.allow("user1", 1000));

        // Time 1000ms: Second request (same time)
        // Queue time: 100ms -> 200ms (will be processed by 1200ms)
        assertTrue(limiter.allow("user1", 1000));

        // Time 1000ms: Third request (same time)
        // Queue time: 200ms -> 300ms (will be processed by 1300ms)
        assertTrue(limiter.allow("user1", 1000));

        // Time 1000ms: Fourth request (same time)
        // Queue time would be 300ms -> 400ms
        // But capacity is 3 (max 300ms queue), so REJECT
        assertFalse(limiter.allow("user1", 1000));
    }

    @Test
    public void testQueueDrainsOverTime() {
        // capacity=3, leak rate=100ms per request
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(3, 100);

        // Time 1000ms: Fill the queue with 3 requests
        assertTrue(limiter.allow("user2", 1000));  // Queue: 0-100ms, done by 1100ms
        assertTrue(limiter.allow("user2", 1000));  // Queue: 100-200ms, done by 1200ms
        assertTrue(limiter.allow("user2", 1000));  // Queue: 200-300ms, done by 1300ms
        assertFalse(limiter.allow("user2", 1000)); // Queue full, reject

        // Time 1100ms: First request has leaked out (processed)
        // nextAvailableTime was 1300ms, now is 1100ms
        // queueDuration = 1300 - 1100 = 200ms (2 requests still in queue)
        // Capacity = 300ms, can add 100ms more (1 request)
        assertTrue(limiter.allow("user2", 1100));  // nextAvailableTime = 1400ms
        assertFalse(limiter.allow("user2", 1100)); // Queue full (300ms capacity)

        // Time 1250ms: 150ms passed since 1100ms
        // nextAvailableTime = 1400ms
        // queueDuration = 1400 - 1250 = 150ms (1.5 requests in queue)
        // Capacity = 300ms, can add 150ms more (1.5 requests worth, so only 1 full request)
        assertTrue(limiter.allow("user2", 1250));  // nextAvailableTime = 1500ms
        assertTrue(limiter.allow("user2", 1250));  // nextAvailableTime = 1600ms (queueDuration=350ms)
        assertFalse(limiter.allow("user2", 1250)); // Would exceed 300ms capacity

        // Time 1400ms: NOT all requests leaked yet!
        // nextAvailableTime = 1600ms
        // queueDuration = 1600 - 1400 = 200ms (2 requests still in queue)
        // Can only add 100ms more (1 request)
        assertTrue(limiter.allow("user2", 1400));  // nextAvailableTime = 1700ms
        assertFalse(limiter.allow("user2", 1400)); // Would be 300ms queue, at limit

        // Time 1700ms: Queue is finally empty
        // Can add 3 new requests
        assertTrue(limiter.allow("user2", 1700));
        assertTrue(limiter.allow("user2", 1700));
        assertTrue(limiter.allow("user2", 1700));
        assertFalse(limiter.allow("user2", 1700));
    }
}