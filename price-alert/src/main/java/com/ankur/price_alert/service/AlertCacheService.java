package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated service for alert caching.
 * Follows Single Responsibility Principle - handles only caching concerns.
 * Implements AlertCacheManager interface for Dependency Inversion.
 */
@Service
public class AlertCacheService implements AlertCacheManager {

    private final PriceAlertRepository alertRepository;

    // Cache: symbol -> TreeMap-indexed alerts
    private final Map<String, CachedAlertIndex> alertCache = new ConcurrentHashMap<>();

    @Value("${alert.cache.ttl.minutes:5}")
    private int cacheTtlMinutes = 5;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AlertCacheService(PriceAlertRepository alertRepository) {
        this.alertRepository = alertRepository;

        // Schedule cache cleanup every minute
        scheduler.scheduleWithFixedDelay(this::cleanupExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Get or build TreeMap index for symbol.
     */
    public CachedAlertIndex getAlertIndexForSymbol(String symbol) {
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

    @Override
    public void invalidateCacheForSymbol(String symbol) {
        alertCache.remove(symbol);
    }

    @Override
    public void invalidateAllCache() {
        alertCache.clear();
    }

    @Override
    public Map<String, Object> getCacheStats() {
        int totalAlerts = alertCache.values().stream()
                .mapToInt(CachedAlertIndex::getTotalAlertCount)
                .sum();

        return Map.of(
                "cachedSymbols", alertCache.size(),
                "totalCachedAlerts", totalAlerts,
                "symbols", alertCache.keySet()
        );
    }

    /**
     * Cleanup expired cache entries.
     */
    private void cleanupExpiredEntries() {
        alertCache.entrySet().removeIf(entry -> entry.getValue().isExpired(cacheTtlMinutes));
    }

    /**
     * Inner class to hold TreeMap-indexed alerts with timestamp.
     * Provides O(log N + K) lookup for alert matching.
     */
    public static class CachedAlertIndex {
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

        public TreeMap<Double, List<PriceAlert>> getPriceAboveAlerts() {
            return priceAboveAlerts;
        }

        public TreeMap<Double, List<PriceAlert>> getPriceBelowAlerts() {
            return priceBelowAlerts;
        }

        public TreeMap<Double, List<PriceAlert>> getPriceEqualsAlerts() {
            return priceEqualsAlerts;
        }

        public TreeMap<Double, List<PriceAlert>> getPriceBetweenAlerts() {
            return priceBetweenAlerts;
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
