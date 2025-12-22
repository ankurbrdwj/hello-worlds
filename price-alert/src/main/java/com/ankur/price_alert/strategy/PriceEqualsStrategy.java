package com.ankur.price_alert.strategy;

import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import org.springframework.stereotype.Component;

/**
 * Strategy for PRICE_EQUALS alert type.
 * Triggers when current price equals the threshold (within 0.1% tolerance).
 */
@Component
public class PriceEqualsStrategy implements AlertTriggerStrategy {

    private static final double TOLERANCE_PERCENT = 0.001; // 0.1% tolerance

    @Override
    public boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        if (alert.getAlertType() != AlertType.PRICE_EQUALS) {
            return false;
        }
        double threshold = alert.getThreshold();
        double tolerance = threshold * TOLERANCE_PERCENT;
        return Math.abs(currentPrice - threshold) <= tolerance;
    }

    @Override
    public String getActionText() {
        return "reached";
    }

    @Override
    public String getExplanation(PriceAlert alert, double currentPrice) {
        return String.format("The price of %s reached your target price of $%.2f (currently at $%.2f).",
                alert.getSymbol(), alert.getThreshold(), currentPrice);
    }
}
