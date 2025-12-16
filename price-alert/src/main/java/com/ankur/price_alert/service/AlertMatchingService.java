package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Matches incoming price ticks against user alerts.
 * Uses Database query with lazy-loading HashMap cache for active symbols.
 *
 * Strategy: DB Query + HashMap Cache (Memory Safe)
 * - Cache only alerts for symbols that received recent price ticks
 * - Evict cache entries after TTL (no ticks for X minutes)
 * - DB is source of truth, cache is for performance
 */
@Service
public class AlertMatchingService {

    private final PriceAlertRepository alertRepository;
    private final EmailService emailService;

    // Cache: symbol -> list of active alerts
    private final Map<String, CachedAlerts> alertCache = new ConcurrentHashMap<>();

    // Track recently triggered alerts to prevent spam
    private final Map<String, Long> recentlyTriggered = new ConcurrentHashMap<>();

    @Value("${alert.cache.ttl.minutes:5}")
    private int cacheTtlMinutes = 5;  // Default value for unit tests

    @Value("${alert.trigger.cooldown.seconds:60}")
    private int triggerCooldownSeconds = 60;  // Default value for unit tests

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AlertMatchingService(PriceAlertRepository alertRepository, EmailService emailService) {
        this.alertRepository = alertRepository;
        this.emailService = emailService;

        // Schedule cache cleanup every minute
        scheduler.scheduleWithFixedDelay(this::cleanupCache, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Process incoming price tick and match against alerts
     */
    public void processPrice(String symbol, double price) {
        try {
            // Get alerts from cache or load from DB
            List<PriceAlert> alerts = getAlertsForSymbol(symbol);

            if (alerts.isEmpty()) {
                return;
            }

            // Check each alert
            for (PriceAlert alert : alerts) {
                if (shouldTrigger(alert, price)) {
                    triggerAlert(alert, price);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing price for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Get alerts for symbol - from cache or DB
     */
    private List<PriceAlert> getAlertsForSymbol(String symbol) {
        CachedAlerts cached = alertCache.get(symbol);

        // Check if cache is valid
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            cached.updateLastAccess();
            return cached.getAlerts();
        }

        // Load from database
        List<PriceAlert> alerts = alertRepository.findBySymbolAndStatus(symbol, AlertStatus.ACTIVE);

        // Update cache
        alertCache.put(symbol, new CachedAlerts(alerts));

        return alerts;
    }

    /**
     * Check if alert should trigger based on price
     */
    private boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        // Check cooldown to prevent spam
        String alertKey = alert.getId() + "-" + alert.getAlertType();
        Long lastTriggered = recentlyTriggered.get(alertKey);
        if (lastTriggered != null) {
            long secondsSinceLastTrigger = (System.currentTimeMillis() - lastTriggered) / 1000;
            if (secondsSinceLastTrigger < triggerCooldownSeconds) {
                return false;
            }
        }

        double threshold = alert.getThreshold();
        AlertType alertType = alert.getAlertType();

        boolean shouldTrigger = switch (alertType) {
            case PRICE_ABOVE -> currentPrice > threshold;
            case PRICE_BELOW -> currentPrice < threshold;
            case PRICE_EQUALS -> {
                double tolerance = threshold * 0.001; // 0.1% tolerance
                yield Math.abs(currentPrice - threshold) <= tolerance;
            }
            case PRICE_BETWEEN -> {
                Double upperThreshold = alert.getUpperThreshold();
                yield upperThreshold != null &&
                      currentPrice >= threshold &&
                      currentPrice <= upperThreshold;
            }
        };

        if (shouldTrigger) {
            recentlyTriggered.put(alertKey, System.currentTimeMillis());
        }

        return shouldTrigger;
    }

    /**
     * Trigger alert - send notification and update status
     */
    private void triggerAlert(PriceAlert alert, double currentPrice) {
        try {
            System.out.println("TRIGGERED: Alert " + alert.getId() +
                    " | " + alert.getSymbol() + " " + alert.getAlertType() +
                    " " + alert.getThreshold() + " | Current: " + currentPrice);

            // Send notification
            emailService.sendPriceAlertNotification(alert, currentPrice);

            // Update alert in database
            alert.setTriggerCount(alert.getTriggerCount() + 1);
            alert.setLastTriggeredAt(LocalDateTime.now());
            alert.setLastTriggerPrice(currentPrice);

            // Deactivate if one-time or max triggers reached
            if (alert.isOneTime() || alert.getTriggerCount() >= alert.getMaxTriggers()) {
                alert.setStatus(AlertStatus.TRIGGERED);
                // Remove from cache
                invalidateCacheForSymbol(alert.getSymbol());
            }

            alertRepository.save(alert);

        } catch (Exception e) {
            System.err.println("Error triggering alert " + alert.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Invalidate cache for a symbol (called when alert is created/deleted/triggered)
     */
    public void invalidateCacheForSymbol(String symbol) {
        alertCache.remove(symbol);
    }

    /**
     * Invalidate entire cache (called on significant changes)
     */
    public void invalidateAllCache() {
        alertCache.clear();
    }

    /**
     * Cleanup expired cache entries
     */
    private void cleanupCache() {
        alertCache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheTtlMinutes));

        // Also cleanup old trigger records
        long cutoff = System.currentTimeMillis() - (triggerCooldownSeconds * 1000L);
        recentlyTriggered.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    /**
     * Get cache stats for monitoring
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "cachedSymbols", alertCache.size(),
                "recentlyTriggeredCount", recentlyTriggered.size(),
                "symbols", alertCache.keySet()
        );
    }

    /**
     * Inner class to hold cached alerts with timestamp
     */
    private static class CachedAlerts {
        private final List<PriceAlert> alerts;
        private final long createdAt;
        private long lastAccessAt;

        public CachedAlerts(List<PriceAlert> alerts) {
            this.alerts = alerts;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessAt = System.currentTimeMillis();
        }

        public List<PriceAlert> getAlerts() {
            return alerts;
        }

        public void updateLastAccess() {
            this.lastAccessAt = System.currentTimeMillis();
        }

        public boolean isExpired(int ttlMinutes) {
            long ageMinutes = (System.currentTimeMillis() - lastAccessAt) / (1000 * 60);
            return ageMinutes >= ttlMinutes;
        }
    }
}