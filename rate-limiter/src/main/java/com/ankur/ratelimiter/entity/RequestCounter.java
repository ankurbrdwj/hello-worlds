package com.ankur.ratelimiter.entity;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the number of requests made by a client against a specific rule.
 * Each RequestCounter is associated with a Client and a RateLimitRule.
 */
public class RequestCounter {

    private String id;
    private String clientId;
    private String ruleId;
    private AtomicLong requestCount;
    private Instant windowStartTime;
    private Instant lastRequestTime;

    public RequestCounter() {
        this.requestCount = new AtomicLong(0);
        this.windowStartTime = Instant.now();
    }

    public RequestCounter(String id, String clientId, String ruleId) {
        this.id = id;
        this.clientId = clientId;
        this.ruleId = ruleId;
        this.requestCount = new AtomicLong(0);
        this.windowStartTime = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public void setRequestCount(long count) {
        this.requestCount.set(count);
    }

    public long incrementAndGet() {
        this.lastRequestTime = Instant.now();
        return requestCount.incrementAndGet();
    }

    public Instant getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(Instant windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public Instant getLastRequestTime() {
        return lastRequestTime;
    }

    public void setLastRequestTime(Instant lastRequestTime) {
        this.lastRequestTime = lastRequestTime;
    }

    public void resetWindow() {
        this.requestCount.set(0);
        this.windowStartTime = Instant.now();
    }

    public long getRemainingRequests(long maxRequests) {
        return Math.max(0, maxRequests - requestCount.get());
    }

    public Instant getWindowResetTime(long windowDurationSeconds) {
        return windowStartTime.plusSeconds(windowDurationSeconds);
    }

    public boolean isWindowExpired(long windowDurationSeconds) {
        return Instant.now().isAfter(windowStartTime.plusSeconds(windowDurationSeconds));
    }

    @Override
    public String toString() {
        return "RequestCounter{" +
                "id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", ruleId='" + ruleId + '\'' +
                ", requestCount=" + requestCount.get() +
                ", windowStartTime=" + windowStartTime +
                ", lastRequestTime=" + lastRequestTime +
                '}';
    }
}