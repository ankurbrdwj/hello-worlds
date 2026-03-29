package com.ankur.bdd.dto;

/**
 * Maps to the response from POST/GET /api/alerts in the price-alert service.
 */
public record AlertResponse(Long id, Long userId, String symbol, String alertType,
                            double threshold, String status) {
}