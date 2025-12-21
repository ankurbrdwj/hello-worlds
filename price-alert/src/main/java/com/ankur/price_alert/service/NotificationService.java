package com.ankur.price_alert.service;

import com.ankur.price_alert.model.PriceAlert;

/**
 * Interface for notification services.
 * Follows Interface Segregation Principle - focused only on notification concerns.
 * Follows Dependency Inversion Principle - high-level modules depend on this abstraction.
 */
public interface NotificationService {

    /**
     * Send a price alert notification to the user.
     *
     * @param alert        the triggered alert
     * @param currentPrice the current price that triggered the alert
     */
    void sendPriceAlertNotification(PriceAlert alert, double currentPrice);

    /**
     * Send a generic notification.
     *
     * @param to      recipient identifier (email, phone, etc.)
     * @param subject notification subject
     * @param body    notification body
     */
    void send(String to, String subject, String body);
}