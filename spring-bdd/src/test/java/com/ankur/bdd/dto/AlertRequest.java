package com.ankur.bdd.dto;

/**
 * Maps to POST /api/alerts in the price-alert service.
 *
 * alertType must be one of: PRICE_ABOVE, PRICE_BELOW, PRICE_EQUALS, PRICE_BETWEEN
 */
public record AlertRequest(Long userId, String symbol, String alertType, double threshold) {
}