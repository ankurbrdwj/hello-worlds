package com.ankur.ratelimiter.service;

public interface RateLimiter {
    boolean allow(String key, long nowMillis);
}
