package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.PriceUpdate;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.ankur.price_alert.model.AlertType.*;

@Service
public class PriceAlertService {

    private final PriceAlertRepository alertRepository;
    private final EmailService emailService;

    private final Set<String> recentlyTriggered = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    public PriceAlertService(PriceAlertRepository alertRepository, EmailService emailService) {
        this.alertRepository = alertRepository;
        this.emailService = emailService;

        // Clean up triggered alerts cache every 5 minutes
        scheduler.scheduleWithFixedDelay(recentlyTriggered::clear, 5, 5, TimeUnit.MINUTES);
    }

    public void evaluate(PriceUpdate priceUpdate) {
        try{
            String symbol = priceUpdate.getSymbol();
            double currentPrice = priceUpdate.getPrice();
            System.out.println("🔍 Evaluating alerts for " + symbol + " at $" + currentPrice);
            // Get all active alerts for this symbol
            List<PriceAlert> activeAlerts = alertRepository.findBySymbolAndStatusActive(symbol);

            if (activeAlerts.isEmpty()) {
                System.out.println("ℹ️ No active alerts for " + symbol);
                return;
            }
            // Check each alert against current price
            for (PriceAlert alert : activeAlerts) {
                if (shouldTriggerAlert(alert, currentPrice)) {
                    triggerAlert(alert, currentPrice);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error evaluating alerts: " + e.getMessage());
        }
    }

    /**
     * Trigger alert and send notification
     */
    private void triggerAlert(PriceAlert alert, double currentPrice) {
        try {
            System.out.println("🚨 TRIGGERING ALERT: " + alert.getId() + " for " + alert.getUserEmail());

            // Send notification asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendPriceAlertNotification(alert, currentPrice);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to send notification: " + e.getMessage());
                }
            });
            // Update alert status
            updateAlertAfterTrigger(alert, currentPrice);

        } catch (Exception e) {
            System.err.println("⚠️ Failed to trigger alert " + alert.getId() + ": " + e.getMessage());
        }
    }


    private boolean shouldTriggerAlert(PriceAlert alert, double currentPrice) {

        // Check if already triggered recently (prevent spam)
        String alertKey = alert.getId() + "-" + (long)(currentPrice * 100); // Price rounded to cents
        if (recentlyTriggered.contains(alertKey)) {
            return false;
        }

        AlertType alertType = alert.getAlertType();
        double threshold = alert.getThreshold();

        boolean shouldTrigger = false;

        switch (alertType) {
            case PRICE_ABOVE:
                shouldTrigger = currentPrice > threshold;
                break;

            case PRICE_BELOW:
                shouldTrigger = currentPrice < threshold;
                break;

            case PRICE_EQUALS:
                // Within 0.1% tolerance
                double tolerance = threshold * 0.001;
                shouldTrigger = Math.abs(currentPrice - threshold) <= tolerance;
                break;

            case PRICE_BETWEEN:
                // For range alerts: threshold stores lower bound, upperThreshold stores upper bound
                shouldTrigger = currentPrice >= threshold && currentPrice <= alert.getUpperThreshold();
                break;

            default:
                System.err.println("⚠️ Unknown alert type: " + alertType);
                return false;
        }

        if (shouldTrigger) {
            // Add to recently triggered to prevent immediate duplicates
            recentlyTriggered.add(alertKey);
        }

        return shouldTrigger;
    }
    private void updateAlertAfterTrigger(PriceAlert alert, double currentPrice) {
        alert.setTriggerCount(alert.getTriggerCount() + 1);
        alert.setLastTriggeredAt(LocalDateTime.now());
        alert.setLastTriggerPrice(currentPrice);

        // Deactivate one-time alerts
        if (alert.isOneTime() || alert.getTriggerCount() >= alert.getMaxTriggers()) {
            alert.setStatus(AlertStatus.TRIGGERED);
            System.out.println("🔄 Deactivated alert: " + alert.getId());
        }

        alertRepository.save(alert);
    }

}
