package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Matches incoming price ticks against user alerts.
 * Uses TreeMap-based indexing for O(log N + K) alert matching.
 *
 * Strategy: TreeMap Index + HashMap Cache
 * - PRICE_ABOVE alerts: TreeMap sorted by threshold, use headMap for O(log N) range query
 * - PRICE_BELOW alerts: TreeMap sorted by threshold, use tailMap for O(log N) range query
 * - PRICE_EQUALS alerts: TreeMap with tolerance-based range query
 * - PRICE_BETWEEN alerts: TreeMap sorted by lower threshold
 *
 * Time Complexity:
 * - Previous: O(N) linear scan per price tick
 * - Now: O(log N + K) where K = number of triggered alerts
 */
@Service
public class AlertMatchingService {

    private final PriceAlertRepository alertRepository;
    private final EmailService emailService;

    // Cache: symbol -> TreeMap-indexed alerts
    private final Map<String, CachedAlertIndex> alertCache = new ConcurrentHashMap<>();

    // Track recently triggered alerts to prevent spam
    private final Map<String, Long> recentlyTriggered = new ConcurrentHashMap<>();

    @Value("${alert.cache.ttl.minutes:5}")
    private int cacheTtlMinutes = 5;

    @Value("${alert.trigger.cooldown.seconds:60}")
    private int triggerCooldownSeconds = 60;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AlertMatchingService(PriceAlertRepository alertRepository, EmailService emailService) {
        this.alertRepository = alertRepository;
        this.emailService = emailService;

        // Schedule cache cleanup every minute
        scheduler.scheduleWithFixedDelay(this::cleanupCache, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Process incoming price tick and match against alerts.
     * Uses TreeMap range queries for O(log N + K) complexity.
     */
    public void processPrice(String symbol, double price) {
        try {
            CachedAlertIndex index = getAlertIndexForSymbol(symbol);

            if (index.isEmpty()) {
                return;
            }

            // Find and trigger matching alerts using TreeMap range queries
            List<PriceAlert> triggeredAlerts = findTriggeredAlerts(index, price);

            for (PriceAlert alert : triggeredAlerts) {
                if (!isInCooldown(alert)) {
                    triggerAlert(alert, price);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing price for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Find all alerts that should trigger for the given price.
     * Uses TreeMap range queries for efficient matching.
     */
    private List<PriceAlert> findTriggeredAlerts(CachedAlertIndex index, double currentPrice) {
        List<PriceAlert> triggered = new ArrayList<>();

        // PRICE_ABOVE: trigger when currentPrice > threshold
        // headMap(price, false) returns all entries with threshold < price
        NavigableMap<Double, List<PriceAlert>> aboveMatches = index.priceAboveAlerts.headMap(currentPrice, false);
        for (List<PriceAlert> alerts : aboveMatches.values()) {
            triggered.addAll(alerts);
        }

        // PRICE_BELOW: trigger when currentPrice < threshold
        // tailMap(price, false) returns all entries with threshold > price
        NavigableMap<Double, List<PriceAlert>> belowMatches = index.priceBelowAlerts.tailMap(currentPrice, false);
        for (List<PriceAlert> alerts : belowMatches.values()) {
            triggered.addAll(alerts);
        }

        // PRICE_EQUALS: trigger when currentPrice ≈ threshold (within 0.1% tolerance)
        // Use subMap to find alerts within tolerance range
        for (Map.Entry<Double, List<PriceAlert>> entry : index.priceEqualsAlerts.entrySet()) {
            double threshold = entry.getKey();
            double tolerance = threshold * 0.001; // 0.1% tolerance
            if (Math.abs(currentPrice - threshold) <= tolerance) {
                triggered.addAll(entry.getValue());
            }
        }

        // PRICE_BETWEEN: trigger when lowerThreshold <= currentPrice <= upperThreshold
        // Check alerts where lower threshold <= currentPrice
        NavigableMap<Double, List<PriceAlert>> betweenCandidates = index.priceBetweenAlerts.headMap(currentPrice, true);
        for (List<PriceAlert> alerts : betweenCandidates.values()) {
            for (PriceAlert alert : alerts) {
                Double upperThreshold = alert.getUpperThreshold();
                if (upperThreshold != null && currentPrice <= upperThreshold) {
                    triggered.add(alert);
                }
            }
        }

        return triggered;
    }

    /**
     * Check if alert is in cooldown period
     */
    private boolean isInCooldown(PriceAlert alert) {
        String alertKey = alert.getId() + "-" + alert.getAlertType();
        Long lastTriggered = recentlyTriggered.get(alertKey);
        if (lastTriggered != null) {
            long secondsSinceLastTrigger = (System.currentTimeMillis() - lastTriggered) / 1000;
            return secondsSinceLastTrigger < triggerCooldownSeconds;
        }
        return false;
    }

    /**
     * Mark alert as triggered for cooldown tracking
     */
    private void markAsTriggered(PriceAlert alert) {
        String alertKey = alert.getId() + "-" + alert.getAlertType();
        recentlyTriggered.put(alertKey, System.currentTimeMillis());
    }

    /**
     * Get or build TreeMap index for symbol
     */
    private CachedAlertIndex getAlertIndexForSymbol(String symbol) {
        CachedAlertIndex cached = alertCache.get(symbol);

        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            cached.updateLastAccess();
            return cached;
        }

        // Load from database and build index
        List<PriceAlert> alerts = alertRepository.findBySymbolAndStatus(symbol, AlertStatus.ACTIVE);
        CachedAlertIndex index = new CachedAlertIndex(alerts);

        alertCache.put(symbol, index);
        return index;
    }

    /**
     * Trigger alert - send notification and update status
     */
    private void triggerAlert(PriceAlert alert, double currentPrice) {
        try {
            System.out.println("TRIGGERED: Alert " + alert.getId() +
                    " | " + alert.getSymbol() + " " + alert.getAlertType() +
                    " " + alert.getThreshold() + " | Current: " + currentPrice);

            // Mark as triggered for cooldown
            markAsTriggered(alert);

            // Send notification
            emailService.sendPriceAlertNotification(alert, currentPrice);

            // Update alert in database
            alert.setTriggerCount(alert.getTriggerCount() + 1);
            alert.setLastTriggeredAt(LocalDateTime.now());
            alert.setLastTriggerPrice(currentPrice);

            // Deactivate if one-time or max triggers reached
            if (alert.isOneTime() || alert.getTriggerCount() >= alert.getMaxTriggers()) {
                alert.setStatus(AlertStatus.TRIGGERED);
                // Remove from cache to rebuild index without this alert
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
        int totalAlerts = alertCache.values().stream()
                .mapToInt(CachedAlertIndex::getTotalAlertCount)
                .sum();

        return Map.of(
                "cachedSymbols", alertCache.size(),
                "totalCachedAlerts", totalAlerts,
                "recentlyTriggeredCount", recentlyTriggered.size(),
                "symbols", alertCache.keySet()
        );
    }

    /**
     * Inner class to hold TreeMap-indexed alerts with timestamp.
     * Provides O(log N + K) lookup for alert matching.
     */
    private static class CachedAlertIndex {
        // TreeMap: threshold -> list of alerts at that threshold
        private final TreeMap<Double, List<PriceAlert>> priceAboveAlerts = new TreeMap<>();
        private final TreeMap<Double, List<PriceAlert>> priceBelowAlerts = new TreeMap<>();
        private final TreeMap<Double, List<PriceAlert>> priceEqualsAlerts = new TreeMap<>();
        private final TreeMap<Double, List<PriceAlert>> priceBetweenAlerts = new TreeMap<>(); // keyed by lower threshold

        private final long createdAt;
        private long lastAccessAt;
        private int totalAlertCount;

        public CachedAlertIndex(List<PriceAlert> alerts) {
            this.createdAt = System.currentTimeMillis();
            this.lastAccessAt = System.currentTimeMillis();
            this.totalAlertCount = alerts.size();

            // Build TreeMap indexes by alert type
            for (PriceAlert alert : alerts) {
                double threshold = alert.getThreshold();
                AlertType type = alert.getAlertType();

                TreeMap<Double, List<PriceAlert>> targetMap = switch (type) {
                    case PRICE_ABOVE -> priceAboveAlerts;
                    case PRICE_BELOW -> priceBelowAlerts;
                    case PRICE_EQUALS -> priceEqualsAlerts;
                    case PRICE_BETWEEN -> priceBetweenAlerts;
                };

                targetMap.computeIfAbsent(threshold, k -> new ArrayList<>()).add(alert);
            }
        }

        public boolean isEmpty() {
            return totalAlertCount == 0;
        }

        public int getTotalAlertCount() {
            return totalAlertCount;
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
