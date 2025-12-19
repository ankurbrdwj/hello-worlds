package com.ankur.gateway.ratelimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Distributed rate limiter using Redis with Sliding Window Log algorithm.
 *
 * Pros:
 * - Distributed (works across multiple gateway instances)
 * - Persistent (survives gateway restarts)
 * - Accurate sliding window
 *
 * Cons:
 * - Requires Redis
 * - Slightly higher latency (network call to Redis)
 *
 * Algorithm: Sliding Window Log
 * - Stores timestamp of each request in a sorted set
 * - Removes timestamps outside the window
 * - Counts remaining timestamps to check limit
 */
public class RedisRateLimiter implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final int limitForPeriod;
    private final Duration windowSize;
    private final String keyPrefix;

    // Lua script for atomic rate limiting operation
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])

            -- Remove old entries outside the window
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

            -- Count current requests in window
            local count = redis.call('ZCARD', key)

            if count < limit then
                -- Add current request timestamp
                redis.call('ZADD', key, now, now .. '-' .. math.random())
                -- Set expiry on the key
                redis.call('PEXPIRE', key, window)
                return {1, limit - count - 1, 0}
            else
                -- Get oldest entry to calculate wait time
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local waitTime = 0
                if #oldest > 0 then
                    waitTime = window - (now - tonumber(oldest[2]))
                end
                return {0, 0, waitTime}
            end
            """;

    private final RedisScript<List> rateLimitScript;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate,
                           int limitForPeriod,
                           Duration windowSize,
                           String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.limitForPeriod = limitForPeriod;
        this.windowSize = windowSize;
        this.keyPrefix = keyPrefix;

        this.rateLimitScript = RedisScript.of(RATE_LIMIT_SCRIPT, List.class);

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║ REDIS RATE LIMITER INITIALIZED                               ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Type          : Distributed (Redis-backed)                   ║");
        log.info("║ Algorithm     : Sliding Window Log                           ║");
        log.info("║ Limit         : {} requests per {}                     ║",
                String.format("%-3d", limitForPeriod),
                String.format("%-6s", windowSize.toSeconds() + "s"));
        log.info("║ Key Prefix    : {}                              ║",
                String.format("%-15s", keyPrefix));
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    @Override
    public RateLimitResult tryAcquire(String key) {
        String redisKey = keyPrefix + ":" + key;
        long now = System.currentTimeMillis();

        try {
            List<Long> result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(now),
                    String.valueOf(windowSize.toMillis()),
                    String.valueOf(limitForPeriod)
            );

            if (result != null && !result.isEmpty()) {
                boolean allowed = result.get(0) == 1L;
                int remaining = result.get(1).intValue();
                long waitTime = result.get(2);

                if (allowed) {
                    log.debug("[REDIS] Key: {} - ALLOWED (remaining: {})", key, remaining);
                    return RateLimitResult.allowed(remaining);
                } else {
                    log.warn("[REDIS] Key: {} - DENIED (wait: {}ms)", key, waitTime);
                    return RateLimitResult.denied(waitTime);
                }
            }

            // Fallback: allow if script fails
            log.error("[REDIS] Script returned null for key: {}, allowing request", key);
            return RateLimitResult.allowed(limitForPeriod);

        } catch (Exception e) {
            // If Redis is down, log error and allow (fail-open)
            log.error("[REDIS] Error checking rate limit for key: {} - {}", key, e.getMessage());
            return RateLimitResult.allowed(limitForPeriod);
        }
    }

    @Override
    public String getType() {
        return "redis";
    }
}