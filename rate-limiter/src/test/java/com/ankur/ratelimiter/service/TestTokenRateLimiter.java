package com.ankur.ratelimiter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token Bucket Rate Limiter:
 * - Bucket has max capacity (e.g., 10 tokens)
 * - Refills at constant rate (e.g., 1 token per 100ms)
 * - Each request consumes 1 token
 * - Allows bursts up to capacity
 */
public class TestTokenRateLimiter {

    @Test
    public void testBasicTokenConsumption() {
        // TokenRateLimiter(capacity, refillTokens, refillTimeMillis)
        //
        // capacity = 3:
        //   Maximum tokens the bucket can hold
        //   Bucket starts FULL with 3 tokens
        //
        // refillTokens = 1:
        //   Number of tokens added during each refill
        //
        // refillTimeMillis = 100:
        //   Time period for refill (1 token every 100ms)
        //
        // So: 1 token is added every 100ms, max 3 tokens total
        TokenRateLimiter limiter = new TokenRateLimiter(3, 1, 100);

        // Bucket starts full with 3 tokens
        // All requests at same time (no refill happens)
        assertTrue(limiter.allow("user1", 1000));  // tokens: 3 -> 2
        assertTrue(limiter.allow("user1", 1000));  // tokens: 2 -> 1
        assertTrue(limiter.allow("user1", 1000));  // tokens: 1 -> 0
        assertFalse(limiter.allow("user1", 1000)); // tokens: 0, reject (no tokens left)
    }

    @Test
    public void testTokenRefill() {
        // capacity=3, refill=1 token per 100ms
        TokenRateLimiter limiter = new TokenRateLimiter(3, 1, 100);

        // Time 1000ms: consume all 3 tokens
        assertTrue(limiter.allow("user2", 1000));  // tokens: 3 -> 2
        assertTrue(limiter.allow("user2", 1000));  // tokens: 2 -> 1
        assertTrue(limiter.allow("user2", 1000));  // tokens: 1 -> 0

        // Time 1050ms: only 50ms passed, not enough for 1 token (need 100ms)
        // tokens = 0 + (50ms / 100ms * 1 token) = 0.5 tokens
        // Need 1 token to allow, so reject
        assertFalse(limiter.allow("user2", 1050)); // tokens: 0.5, reject

        // Time 1100ms: 100ms passed, refill 1 token
        // tokens = 0 + (100ms / 100ms * 1 token) = 1.0 token
        assertTrue(limiter.allow("user2", 1100));  // tokens: 1.0 -> 0.0, allow

        // Time 1300ms: 200ms passed since last request at 1100ms
        // tokens = 0 + (200ms / 100ms * 1 token) = 2.0 tokens
        assertTrue(limiter.allow("user2", 1300));  // tokens: 2.0 -> 1.0, allow
        assertTrue(limiter.allow("user2", 1300));  // tokens: 1.0 -> 0.0, allow
        assertFalse(limiter.allow("user2", 1300)); // tokens: 0.0, reject
    }
}