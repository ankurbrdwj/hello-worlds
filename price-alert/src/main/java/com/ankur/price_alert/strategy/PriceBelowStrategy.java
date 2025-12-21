package com.ankur.price_alert.strategy;

import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import org.springframework.stereotype.Component;

/**
 * Strategy for PRICE_BELOW alert type.
 * Triggers when current price drops below the threshold.
 */
@Component
public class PriceBelowStrategy implements AlertTriggerStrategy {

    @Override
    public boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        if (alert.getAlertType() != AlertType.PRICE_BELOW) {
            return false;
        }
        return currentPrice < alert.getThreshold();
    }

    @Override
    public String getActionText() {
        return "dropped below";
    }

    @Override
    public String getExplanation(PriceAlert alert, double currentPrice) {
        return String.format("The price of %s dropped below your threshold of $%.2f, reaching $%.2f.",
                alert.getSymbol(), alert.getThreshold(), currentPrice);
    }
}
