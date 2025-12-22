package com.ankur.gateway.ratelimiter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using Resilience4j.
 *
 * Pros:
 * - No external dependencies (Redis not required)
 * - Very fast (in-memory)
 * - Good for single instance deployments
 *
 * Cons:
 * - Not distributed (each gateway instance has its own limits)
 * - Limits reset if gateway restarts
 */
public class Resilience4jRateLimiter implements RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jRateLimiter.class);

    private final RateLimiterRegistry registry;
    private final RateLimiterConfig defaultConfig;
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public Resilience4jRateLimiter(int limitForPeriod, Duration limitRefreshPeriod, Duration timeoutDuration) {
        // Set timeout to ZERO for immediate rejection (non-blocking)
        this.defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(limitRefreshPeriod)
                .timeoutDuration(Duration.ZERO)  // Don't wait, reject immediately
                .build();

        this.registry = RateLimiterRegistry.of(defaultConfig);

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║ RESILIENCE4J RATE LIMITER INITIALIZED                        ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ Type          : In-Memory (non-distributed)                  ║");
        log.info("║ Limit         : {} requests per {}                     ║",
                String.format("%-3d", limitForPeriod),
                String.format("%-6s", limitRefreshPeriod.toSeconds() + "s"));
        log.info("║ Timeout       : {}                                       ║",
                String.format("%-5s", timeoutDuration.toMillis() + "ms"));
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    @Override
    public RateLimitResult tryAcquire(String key) {
        RateLimiter rateLimiter = limiters.computeIfAbsent(key,
                k -> registry.rateLimiter(k, defaultConfig));

        // acquirePermission() with timeoutDuration=0 returns immediately (non-blocking)
        boolean permitted = rateLimiter.acquirePermission();

        if (permitted) {
            int remaining = rateLimiter.getMetrics().getAvailablePermissions();
            log.debug("[RESILIENCE4J] Key: {} - ALLOWED (remaining: {})", key, remaining);
            return RateLimitResult.allowed(remaining);
        } else {
            // Calculate wait time based on refresh period
            long waitTime = defaultConfig.getLimitRefreshPeriod().toMillis();
            log.warn("[RESILIENCE4J] Key: {} - DENIED (wait: {}ms)", key, waitTime);
            return RateLimitResult.denied(waitTime);
        }
    }

    @Override
    public String getType() {
        return "resilience4j";
    }
}