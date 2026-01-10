package com.ankur.billing.period.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Production-Ready Redis Cache Configuration
 *
 * Key Cache Invalidation Strategies Demonstrated:
 * 1. TTL (Time To Live): Auto-expire cache after X minutes
 * 2. Manual invalidation: @CacheEvict when data changes
 * 3. Cache warming: Pre-populate cache on startup
 *
 * Common Production Cache Invalidation Problems:
 *
 * PROBLEM 1: Stale Data
 * - Cache holds old data after source changes
 * - Multiple services share cache, who invalidates?
 * - Solution: TTL + event-driven invalidation
 *
 * PROBLEM 2: Cache Stampede (Thundering Herd)
 * - Cache expires, 1000 requests hit DB simultaneously
 * - Database gets overwhelmed
 * - Solution: Staggered TTL, cache locking, refresh-ahead
 *
 * PROBLEM 3: Cache Consistency
 * - Related caches get out of sync
 * - Example: Individual period cache vs all-periods cache
 * - Solution: Invalidate related caches together
 *
 * PROBLEM 4: Memory Pressure
 * - Cache grows too large, Redis OOM
 * - Solution: maxmemory-policy, LRU eviction
 */
@Configuration
@EnableCaching
public class periodCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure ObjectMapper to handle Java 8 Date/Time types (LocalDate, etc.)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Default configuration for all caches
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // TTL = 10 minutes (Strategy #1: Time-based invalidation)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)
                        )
                );

        // Specific cache configurations with different TTLs
        RedisCacheConfiguration billingPeriodsConfig = defaultConfig
                .entryTtl(Duration.ofHours(1)); // Billing periods change rarely

        RedisCacheConfiguration yearPeriodsConfig = defaultConfig
                .entryTtl(Duration.ofHours(24)); // Year data changes very rarely

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("billingPeriods", billingPeriodsConfig)
                .withCacheConfiguration("yearPeriods", yearPeriodsConfig)
                .build();
    }
}