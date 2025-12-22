package com.ankur.price_alert.service;

import java.util.Map;

/**
 * Interface for alert cache management.
 * Follows Interface Segregation Principle - separates cache concerns from processing.
 */
public interface AlertCacheManager {

    /**
     * Invalidate cache for a specific symbol.
     *
     * @param symbol the stock symbol
     */
    void invalidateCacheForSymbol(String symbol);

    /**
     * Invalidate entire cache.
     */
    void invalidateAllCache();

    /**
     * Get cache statistics for monitoring.
     *
     * @return map of cache statistics
     */
    Map<String, Object> getCacheStats();
}