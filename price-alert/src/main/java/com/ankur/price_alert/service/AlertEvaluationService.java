package com.ankur.price_alert.service;

import com.ankur.price_alert.model.PriceUpdate;

/**
 * Interface for alert evaluation services.
 * Follows Interface Segregation Principle - focused only on evaluation concerns.
 * Follows Dependency Inversion Principle - consumers depend on this abstraction.
 */
public interface AlertEvaluationService {

    /**
     * Evaluate all active alerts against the given price update.
     *
     * @param priceUpdate the price update to evaluate
     */
    void evaluate(PriceUpdate priceUpdate);
}