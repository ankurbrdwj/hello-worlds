package com.ankur.price_alert.strategy;

import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for obtaining the appropriate AlertTriggerStrategy based on alert type.
 * Follows Open/Closed Principle - new strategies are automatically registered via Spring DI.
 * Follows Factory Pattern - centralizes strategy creation/lookup.
 */
@Component
public class AlertStrategyFactory {

    private final Map<AlertType, AlertTriggerStrategy> strategyMap;
    private final AlertTriggerStrategy defaultStrategy;

    public AlertStrategyFactory(List<AlertTriggerStrategy> strategies) {
        this.strategyMap = new EnumMap<>(AlertType.class);

        // Register strategies by inspecting their type handling
        for (AlertTriggerStrategy strategy : strategies) {
            if (strategy instanceof PriceAboveStrategy) {
                strategyMap.put(AlertType.PRICE_ABOVE, strategy);
            } else if (strategy instanceof PriceBelowStrategy) {
                strategyMap.put(AlertType.PRICE_BELOW, strategy);
            } else if (strategy instanceof PriceEqualsStrategy) {
                strategyMap.put(AlertType.PRICE_EQUALS, strategy);
            } else if (strategy instanceof PriceBetweenStrategy) {
                strategyMap.put(AlertType.PRICE_BETWEEN, strategy);
            }
        }

        // Default strategy that never triggers (for unknown types)
        this.defaultStrategy = new AlertTriggerStrategy() {
            @Override
            public boolean shouldTrigger(PriceAlert alert, double currentPrice) {
                return false;
            }

            @Override
            public String getActionText() {
                return "triggered at";
            }

            @Override
            public String getExplanation(PriceAlert alert, double currentPrice) {
                return String.format("Your price alert condition for %s has been met.", alert.getSymbol());
            }
        };
    }

    /**
     * Get the appropriate strategy for the given alert type.
     *
     * @param alertType the alert type
     * @return the corresponding strategy, or default if not found
     */
    public AlertTriggerStrategy getStrategy(AlertType alertType) {
        return strategyMap.getOrDefault(alertType, defaultStrategy);
    }

    /**
     * Evaluate if an alert should trigger using the appropriate strategy.
     *
     * @param alert        the alert to evaluate
     * @param currentPrice the current price
     * @return true if the alert should trigger
     */
    public boolean shouldTrigger(PriceAlert alert, double currentPrice) {
        AlertTriggerStrategy strategy = getStrategy(alert.getAlertType());
        return strategy.shouldTrigger(alert, currentPrice);
    }

    /**
     * Get action text for an alert type.
     *
     * @param alertType the alert type
     * @return the action text
     */
    public String getActionText(AlertType alertType) {
        return getStrategy(alertType).getActionText();
    }

    /**
     * Get explanation for a triggered alert.
     *
     * @param alert        the triggered alert
     * @param currentPrice the current price
     * @return the explanation text
     */
    public String getExplanation(PriceAlert alert, double currentPrice) {
        return getStrategy(alert.getAlertType()).getExplanation(alert, currentPrice);
    }
}
