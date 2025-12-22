package com.ankur.ratelimiter.entity;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Defines rate limiting policies.
 * Each rule specifies max requests allowed within a time window,
 * which clients it applies to, and what endpoints it covers.
 */
public class RateLimitRule {

    private String id;
    private String name;
    private long maxRequests;
    private Duration timeWindow;
    private String endpointPattern;
    private Client.ClientType applicableClientType;
    private boolean enabled;

    public RateLimitRule() {
        this.enabled = true;
    }

    public RateLimitRule(String id, String name, long maxRequests, Duration timeWindow) {
        this.id = id;
        this.name = name;
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        this.enabled = true;
    }

    public static RateLimitRule perMinute(String id, String name, long maxRequests) {
        return new RateLimitRule(id, name, maxRequests, Duration.of(1, ChronoUnit.MINUTES));
    }

    public static RateLimitRule perHour(String id, String name, long maxRequests) {
        return new RateLimitRule(id, name, maxRequests, Duration.of(1, ChronoUnit.HOURS));
    }

    public static RateLimitRule perSecond(String id, String name, long maxRequests) {
        return new RateLimitRule(id, name, maxRequests, Duration.of(1, ChronoUnit.SECONDS));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(long maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Duration getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(Duration timeWindow) {
        this.timeWindow = timeWindow;
    }

    public long getTimeWindowInSeconds() {
        return timeWindow.getSeconds();
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public void setEndpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
    }

    public Client.ClientType getApplicableClientType() {
        return applicableClientType;
    }

    public void setApplicableClientType(Client.ClientType applicableClientType) {
        this.applicableClientType = applicableClientType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "RateLimitRule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", maxRequests=" + maxRequests +
                ", timeWindow=" + timeWindow +
                ", endpointPattern='" + endpointPattern + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}