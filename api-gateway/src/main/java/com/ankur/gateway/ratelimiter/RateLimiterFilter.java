package com.ankur.gateway.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/**
 * Rate Limiter Filter for Gateway routes.
 *
 * Extracts client identifier (IP, API key, etc.) and checks rate limit.
 * Returns 429 Too Many Requests if limit exceeded.
 */
@Component
public class RateLimiterFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    private final RateLimiterService rateLimiterService;

    public RateLimiterFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
        log.info("RateLimiterFilter initialized with: {}", rateLimiterService.getType());
    }

    /**
     * Check rate limit before forwarding request.
     * Returns the request if allowed, or throws exception if denied.
     */
    public ServerRequest filter(ServerRequest request) {
        String clientKey = extractClientKey(request);
        RateLimitResult result = rateLimiterService.tryAcquire(clientKey);

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for client: {} - wait {}ms",
                    clientKey, result.getWaitTimeMillis());
            throw new RateLimitExceededException(result);
        }

        log.debug("Rate limit check passed for client: {} (remaining: {})",
                clientKey, result.getRemainingTokens());
        return request;
    }

    /**
     * Handle rate limit exceeded - returns 429 response.
     */
    public ServerResponse handleRateLimitExceeded(RateLimitExceededException ex, ServerRequest request) {
        RateLimitResult result = ex.getResult();

        return ServerResponse.status(429)
                .header("X-RateLimit-Remaining", "0")
                .header("X-RateLimit-Retry-After-Ms", String.valueOf(result.getWaitTimeMillis()))
                .header("X-RateLimit-Type", rateLimiterService.getType())
                .body(Map.of(
                        "error", "Too Many Requests",
                        "message", result.getMessage(),
                        "retryAfterMs", result.getWaitTimeMillis()
                ));
    }

    /**
     * Extract client identifier from request.
     * Priority: X-API-Key header > X-Forwarded-For > Remote IP
     */
    private String extractClientKey(ServerRequest request) {
        // Check for API key header
        String apiKey = request.headers().firstHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "api:" + apiKey;
        }

        // Check for forwarded IP (behind load balancer/proxy)
        String forwardedFor = request.headers().firstHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Take first IP in chain
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        HttpServletRequest servletRequest = request.servletRequest();
        return "ip:" + servletRequest.getRemoteAddr();
    }

    /**
     * Get the type of rate limiter being used.
     */
    public String getRateLimiterType() {
        return rateLimiterService.getType();
    }

    /**
     * Exception thrown when rate limit is exceeded.
     */
    public static class RateLimitExceededException extends RuntimeException {
        private final RateLimitResult result;

        public RateLimitExceededException(RateLimitResult result) {
            super(result.getMessage());
            this.result = result;
        }

        public RateLimitResult getResult() {
            return result;
        }
    }
}