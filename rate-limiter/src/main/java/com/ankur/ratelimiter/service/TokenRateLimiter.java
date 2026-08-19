package com.ankur.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket Rate Limiter
 *
 * How it works:
 * - Bucket holds tokens (max capacity)
 * - Tokens refill at a constant rate
 * - Each request consumes 1 token
 * - If no tokens available → reject
 *
 * Benefits:
 * - ✅ Allows bursts (up to bucket capacity)
 * - ✅ Smooth long-term rate
 * - ✅ Simple to implement
 * - ✅ Used by AWS, Stripe, Shopify
 */
public class TokenRateLimiter implements RateLimiter{

    private final int refillTokens;      // Tokens added per refill period
    private final int capacity;          // Max tokens in bucket
    private final long refillTime;       // Refill period in milliseconds

    private final Map<String, BucketData> buckets;

    public TokenRateLimiter(int capacity, int refillTokens, int refillTime) {
           this.capacity=capacity;
           this.refillTokens=refillTokens;
           this.refillTime=refillTime;
           this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allow(String key, long nowMillis) {
        // Each key gets its own independent bucket — isolation means one client's burst
        // does not consume tokens from another client's bucket.
        BucketData bucket = buckets.computeIfAbsent(key, k -> new BucketData(capacity, nowMillis));

        // ReentrantLock instead of synchronized: allows tryLock() for non-blocking checks
        // and supports a fairness policy (new ReentrantLock(true)) to prevent starvation
        // under high contention on the same key. Different keys lock different buckets,
        // so they never block each other.
        bucket.lock.lock();
        try {
            // Step 1: Calculate time passed since last refill
            long timePassed = nowMillis - bucket.lastRefillTime;

            // Step 2: Calculate how many tokens to add
            // Formula: (timePassed / refillTime) * refillTokens
            double tokensToAdd = (timePassed / (double) refillTime) * refillTokens;

            // Step 3: Refill tokens (capped at capacity)
            bucket.tokens = Math.min(capacity, bucket.tokens + tokensToAdd);
            bucket.lastRefillTime = nowMillis;

            // Step 4: Try to consume 1 token
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;  // Allow request
            }

            return false;  // Reject - not enough tokens
        } finally {
            bucket.lock.unlock();
        }
    }

    // Bucket data per user/key
    private static class BucketData {
        double tokens;           // Current tokens (can be fractional)
        long lastRefillTime;     // Last time we refilled
        // One lock per bucket: threads competing for the same key serialize here,
        // while threads on different keys proceed in parallel without contention.
        final ReentrantLock lock = new ReentrantLock();

        BucketData(int capacity, long nowMillis) {
            this.tokens = capacity;  // Start full
            this.lastRefillTime = nowMillis;
        }
    }
}
