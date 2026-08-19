package com.ankur.cache.redis.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDataLoader implements ApplicationRunner {

    private static final int FILE_COUNT = 16;

    @Value("${loader.object-size-mb:20}")
    private int objectSizeMb;

    private final RedisTemplate<String, byte[]> redisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int objectSize = objectSizeMb * 1024 * 1024;
        log.info("Pre-loading {} keys x {}MB into Redis ...", FILE_COUNT, objectSizeMb);
        for (int i = 0; i < FILE_COUNT; i++) {
            String key = String.format("file_%02d.dat", i);
            // Zero-filled: fast to allocate, no randomness needed for the OOM demo
            redisTemplate.opsForValue().set(key, new byte[objectSize]);
            log.info("  loaded key={} ({}MB)", key, objectSizeMb);
        }
        log.info("Redis pre-load complete — {} keys ready", FILE_COUNT);
    }
}