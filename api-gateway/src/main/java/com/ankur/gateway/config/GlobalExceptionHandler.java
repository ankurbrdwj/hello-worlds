package com.ankur.gateway.config;

import com.ankur.gateway.ratelimiter.RateLimiterFilter;
import com.ankur.gateway.ratelimiter.RateLimitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimiterFilter.RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimiterFilter.RateLimitExceededException ex) {

        RateLimitResult result = ex.getResult();

        log.warn("Rate limit exceeded - returning 429: {}", result.getMessage());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Remaining", "0")
                .header("X-RateLimit-Retry-After-Ms", String.valueOf(result.getWaitTimeMillis()))
                .header("Retry-After", String.valueOf(result.getWaitTimeMillis() / 1000))
                .body(Map.of(
                        "error", "Too Many Requests",
                        "status", 429,
                        "message", result.getMessage(),
                        "retryAfterMs", result.getWaitTimeMillis()
                ));
    }
}