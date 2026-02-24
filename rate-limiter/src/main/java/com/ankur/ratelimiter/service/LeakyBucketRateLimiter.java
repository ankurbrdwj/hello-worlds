package com.ankur.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Leaky Bucket Rate Limiter
 *
 * How it works:
 * - Requests queue in a bucket
 * - Bucket "leaks" (processes) at constant rate
 * - If bucket is full, reject new requests
 *
 * Difference from Token Bucket:
 * - Token Bucket: Allows bursts (consume tokens instantly)
 * - Leaky Bucket: Constant rate (requests processed at fixed intervals)
 *
 * Benefits:
 * - ✅ Smooth, predictable traffic
 * - ✅ No bursts
 * - ✅ Good for rate-sensitive APIs
 */
public class LeakyBucketRateLimiter implements RateLimiter {

    private final int capacity;          // Max requests in queue
    private final long leakRateMillis;   // Time to process each request

    private final Map<String, BucketData> buckets;

    public LeakyBucketRateLimiter(int capacity, long leakRateMillis) {
        this.capacity = capacity;
        this.leakRateMillis = leakRateMillis;
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allow(String key, long nowMillis) {
        // Get or create bucket for this key
        BucketData bucket = buckets.computeIfAbsent(key, k -> new BucketData(nowMillis));

        synchronized (bucket) {
            // Step 1: Calculate when the queue will be empty
            // If current time is past nextAvailableTime, queue is empty
            long queueEmptyTime = Math.max(bucket.nextAvailableTime, nowMillis);

            // Step 2: Calculate queue duration (how long requests are queued)
            long queueDuration = queueEmptyTime - nowMillis;

            // Step 3: Check if adding this request would exceed capacity
            long maxQueueDuration = capacity * leakRateMillis;
            if (queueDuration >= maxQueueDuration) {
                return false;  // Queue is full, reject
            }

            // Step 4: Add this request to the queue
            // It will be processed after all queued requests
            bucket.nextAvailableTime = queueEmptyTime + leakRateMillis;
            return true;
        }
    }

    // Bucket data per user/key
    private static class BucketData {
        long nextAvailableTime;  // When the next request can be processed

        BucketData(long nowMillis) {
            this.nextAvailableTime = nowMillis;
        }
    }
}