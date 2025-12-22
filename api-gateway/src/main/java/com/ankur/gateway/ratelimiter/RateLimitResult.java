package com.ankur.gateway.ratelimiter;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RateLimitResult {

    private final boolean allowed;
    private final int remainingTokens;
    private final long waitTimeMillis;
    private final String message;

    public static RateLimitResult allowed(int remaining) {
        return RateLimitResult.builder()
                .allowed(true)
                .remainingTokens(remaining)
                .waitTimeMillis(0)
                .message("Request allowed")
                .build();
    }

    public static RateLimitResult denied(long waitTimeMillis) {
        return RateLimitResult.builder()
                .allowed(false)
                .remainingTokens(0)
                .waitTimeMillis(waitTimeMillis)
                .message("Rate limit exceeded. Retry after " + waitTimeMillis + "ms")
                .build();
    }
}