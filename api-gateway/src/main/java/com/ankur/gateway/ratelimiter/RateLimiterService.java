package com.ankur.gateway.ratelimiter;

/**
 * Rate Limiter interface - implementations can use Resilience4j (in-memory)
 * or Redis (distributed).
 *
 * Switch between implementations using:
 * gateway.ratelimiter.type=resilience4j  (default, in-memory)
 * gateway.ratelimiter.type=redis         (distributed)
 */
public interface RateLimiterService {

    /**
     * Check if request is allowed for the given key.
     *
     * @param key - unique identifier (e.g., IP address, API key, user ID)
     * @return RateLimitResult with allowed status and metadata
     */
    RateLimitResult tryAcquire(String key);

    /**
     * Get the type of rate limiter implementation.
     */
    String getType();
}