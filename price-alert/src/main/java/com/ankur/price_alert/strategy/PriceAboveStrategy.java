package com.ankur.price_alert.strategy;

import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import org.springframework.stereotype.Component;

/**
 * Strategy for PRICE_ABOVE alert type.
 * Triggers when current price exceeds the threshold.
 */
@Component
public class PriceAboveStrategy implements AlertTriggerStrategy {

    @Override
    public boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        if (alert.getAlertType() != AlertType.PRICE_ABOVE) {
            return false;
        }
        return currentPrice > alert.getThreshold();
    }

    @Override
    public String getActionText() {
        return "rose above";
    }

    @Override
    public String getExplanation(PriceAlert alert, double currentPrice) {
        return String.format("The price of %s rose above your threshold of $%.2f, reaching $%.2f.",
                alert.getSymbol(), alert.getThreshold(), currentPrice);
    }
}
