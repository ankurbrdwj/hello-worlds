package com.ankur.ratelimiter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSlidingWindowLogLimiter {
    @Test
    public void testSlidingWindow() {
        RateLimiter limiter = new LogLimiterImpl(3, 100);
        assertTrue(limiter.allow("user123", 1000));
        assertTrue(limiter.allow("user123", 1010));
        assertTrue(limiter.allow("user123", 1020));
        assertFalse(limiter.allow("user123", 1030));
    }
}
