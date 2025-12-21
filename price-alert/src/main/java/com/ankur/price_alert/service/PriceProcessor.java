package com.ankur.price_alert.service;

import java.util.Map;

/**
 * Interface for price processing services.
 * Follows Interface Segregation Principle - separates price processing from cache management.
 */
public interface PriceProcessor {

    /**
     * Process incoming price tick and match against alerts.
     *
     * @param symbol the stock symbol
     * @param price  the current price
     */
    void processPrice(String symbol, double price);
}