package com.ankur.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        // Get or create bucket for this key (starts full)
        BucketData bucket = buckets.computeIfAbsent(key, k -> new BucketData(capacity, nowMillis));

        synchronized (bucket) {
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
        }
    }

    // Bucket data per user/key
    private static class BucketData {
        double tokens;           // Current tokens (can be fractional)
        long lastRefillTime;     // Last time we refilled

        BucketData(int capacity, long nowMillis) {
            this.tokens = capacity;  // Start full
            this.lastRefillTime = nowMillis;
        }
    }
}
