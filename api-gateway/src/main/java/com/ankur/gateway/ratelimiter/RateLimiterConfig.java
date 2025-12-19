package com.ankur.gateway.ratelimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "gateway.ratelimiter")
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    /**
     * Type of rate limiter: "resilience4j" or "redis"
     */
    private String type = "resilience4j";

    /**
     * Number of requests allowed per window
     */
    private int limit = 100;

    /**
     * Window duration in seconds
     */
    private int windowSeconds = 1;

    /**
     * Timeout for acquiring permit (Resilience4j only)
     */
    private int timeoutMillis = 500;

    /**
     * Redis key prefix (Redis only)
     */
    private String redisKeyPrefix = "rate-limit";

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

    public int getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(int timeoutMillis) { this.timeoutMillis = timeoutMillis; }

    public String getRedisKeyPrefix() { return redisKeyPrefix; }
    public void setRedisKeyPrefix(String redisKeyPrefix) { this.redisKeyPrefix = redisKeyPrefix; }

    /**
     * Resilience4j Rate Limiter - used when gateway.ratelimiter.type=resilience4j (default)
     */
    @Bean
    @ConditionalOnProperty(name = "gateway.ratelimiter.type", havingValue = "resilience4j", matchIfMissing = true)
    public RateLimiterService resilience4jRateLimiter() {
        log.info("Creating Resilience4j Rate Limiter (in-memory)");
        return new Resilience4jRateLimiter(
                limit,
                Duration.ofSeconds(windowSeconds),
                Duration.ofMillis(timeoutMillis)
        );
    }

    /**
     * Redis Rate Limiter - used when gateway.ratelimiter.type=redis
     */
    @Bean
    @ConditionalOnProperty(name = "gateway.ratelimiter.type", havingValue = "redis")
    public RateLimiterService redisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("Creating Redis Rate Limiter (distributed)");
        return new RedisRateLimiter(
                redisTemplate,
                limit,
                Duration.ofSeconds(windowSeconds),
                redisKeyPrefix
        );
    }

    /**
     * Redis Template for rate limiting
     */
    @Bean
    @ConditionalOnProperty(name = "gateway.ratelimiter.type", havingValue = "redis")
    public RedisTemplate<String, String> rateLimiterRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}