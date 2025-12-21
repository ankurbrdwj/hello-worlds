package com.ankur.price_alert.strategy;

import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import org.springframework.stereotype.Component;

/**
 * Strategy for PRICE_BETWEEN alert type.
 * Triggers when current price falls within the specified range.
 */
@Component
public class PriceBetweenStrategy implements AlertTriggerStrategy {

    @Override
    public boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        if (alert.getAlertType() != AlertType.PRICE_BETWEEN) {
            return false;
        }
        Double upperThreshold = alert.getUpperThreshold();
        if (upperThreshold == null) {
            return false;
        }
        return currentPrice >= alert.getThreshold() && currentPrice <= upperThreshold;
    }

    @Override
    public String getActionText() {
        return "moved within range";
    }

    @Override
    public String getExplanation(PriceAlert alert, double currentPrice) {
        return String.format("The price of %s moved within your specified range ($%.2f - $%.2f), currently at $%.2f.",
                alert.getSymbol(), alert.getThreshold(), alert.getUpperThreshold(), currentPrice);
    }
}
