package com.ankur.price_alert.exception;

/**
 * Exception thrown when an alert is not found.
 */
public class AlertNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "ALERT_NOT_FOUND";

    public AlertNotFoundException(Long alertId) {
        super("Alert", alertId, ERROR_CODE);
    }
}
