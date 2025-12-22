package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
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
 * Implements PriceProcessor interface for price processing.
 * Delegates cache management to AlertCacheService (Single Responsibility Principle).
 *
 * Time Complexity: O(log N + K) where K = number of triggered alerts.
 */
@Service
public class AlertMatchingService implements PriceProcessor {

    private final PriceAlertRepository alertRepository;
    private final NotificationService notificationService;
    private final AlertCacheService alertCacheService;

    // Track recently triggered alerts to prevent spam
    private final Map<String, Long> recentlyTriggered = new ConcurrentHashMap<>();

    @Value("${alert.trigger.cooldown.seconds:60}")
    private int triggerCooldownSeconds = 60;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AlertMatchingService(PriceAlertRepository alertRepository,
                                NotificationService notificationService,
                                AlertCacheService alertCacheService) {
        this.alertRepository = alertRepository;
        this.notificationService = notificationService;
        this.alertCacheService = alertCacheService;

        // Schedule cooldown cleanup every minute
        scheduler.scheduleWithFixedDelay(this::cleanupCooldowns, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Process incoming price tick and match against alerts.
     * Uses TreeMap range queries for O(log N + K) complexity.
     */
    @Override
    public void processPrice(String symbol, double price) {
        try {
            AlertCacheService.CachedAlertIndex index = alertCacheService.getAlertIndexForSymbol(symbol);

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
    private List<PriceAlert> findTriggeredAlerts(AlertCacheService.CachedAlertIndex index, double currentPrice) {
        List<PriceAlert> triggered = new ArrayList<>();

        // PRICE_ABOVE: trigger when currentPrice > threshold
        NavigableMap<Double, List<PriceAlert>> aboveMatches =
                index.getPriceAboveAlerts().headMap(currentPrice, false);
        for (List<PriceAlert> alerts : aboveMatches.values()) {
            triggered.addAll(alerts);
        }

        // PRICE_BELOW: trigger when currentPrice < threshold
        NavigableMap<Double, List<PriceAlert>> belowMatches =
                index.getPriceBelowAlerts().tailMap(currentPrice, false);
        for (List<PriceAlert> alerts : belowMatches.values()) {
            triggered.addAll(alerts);
        }

        // PRICE_EQUALS: trigger when currentPrice ≈ threshold (within 0.1% tolerance)
        for (Map.Entry<Double, List<PriceAlert>> entry : index.getPriceEqualsAlerts().entrySet()) {
            double threshold = entry.getKey();
            double tolerance = threshold * 0.001; // 0.1% tolerance
            if (Math.abs(currentPrice - threshold) <= tolerance) {
                triggered.addAll(entry.getValue());
            }
        }

        // PRICE_BETWEEN: trigger when lowerThreshold <= currentPrice <= upperThreshold
        NavigableMap<Double, List<PriceAlert>> betweenCandidates =
                index.getPriceBetweenAlerts().headMap(currentPrice, true);
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
     * Check if alert is in cooldown period.
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
     * Mark alert as triggered for cooldown tracking.
     */
    private void markAsTriggered(PriceAlert alert) {
        String alertKey = alert.getId() + "-" + alert.getAlertType();
        recentlyTriggered.put(alertKey, System.currentTimeMillis());
    }

    /**
     * Trigger alert - send notification and update status.
     */
    private void triggerAlert(PriceAlert alert, double currentPrice) {
        try {
            System.out.println("TRIGGERED: Alert " + alert.getId() +
                    " | " + alert.getSymbol() + " " + alert.getAlertType() +
                    " " + alert.getThreshold() + " | Current: " + currentPrice);

            // Mark as triggered for cooldown
            markAsTriggered(alert);

            // Send notification via interface
            notificationService.sendPriceAlertNotification(alert, currentPrice);

            // Update alert in database
            alert.setTriggerCount(alert.getTriggerCount() + 1);
            alert.setLastTriggeredAt(LocalDateTime.now());
            alert.setLastTriggerPrice(currentPrice);

            // Deactivate if one-time or max triggers reached
            if (alert.isOneTime() || alert.getTriggerCount() >= alert.getMaxTriggers()) {
                alert.setStatus(AlertStatus.TRIGGERED);
                // Remove from cache to rebuild index without this alert
                alertCacheService.invalidateCacheForSymbol(alert.getSymbol());
            }

            alertRepository.save(alert);

        } catch (Exception e) {
            System.err.println("Error triggering alert " + alert.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Cleanup old cooldown records.
     */
    private void cleanupCooldowns() {
        long cutoff = System.currentTimeMillis() - (triggerCooldownSeconds * 1000L);
        recentlyTriggered.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    /**
     * Get cooldown stats for monitoring.
     */
    public int getRecentlyTriggeredCount() {
        return recentlyTriggered.size();
    }
}
