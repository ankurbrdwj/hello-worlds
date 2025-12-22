package com.ankur.price_alert.strategy;

import com.ankur.price_alert.model.PriceAlert;

/**
 * Strategy interface for determining if an alert should trigger.
 * Follows Open/Closed Principle - new alert types can be added without modifying existing code.
 * Follows Single Responsibility Principle - each strategy handles one alert type logic.
 */
public interface AlertTriggerStrategy {

    /**
     * Determine if the alert should trigger based on the current price.
     *
     * @param alert        the price alert to evaluate
     * @param currentPrice the current market price
     * @return true if the alert should trigger, false otherwise
     */
    boolean shouldTrigger(PriceAlert alert, double currentPrice);

    /**
     * Get human-readable description of the trigger action.
     *
     * @return action text (e.g., "rose above", "dropped below")
     */
    String getActionText();

    /**
     * Get detailed explanation of why the alert triggered.
     *
     * @param alert        the triggered alert
     * @param currentPrice the current price
     * @return explanation text
     */
    String getExplanation(PriceAlert alert, double currentPrice);
}
