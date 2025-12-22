package com.ankur.price_alert.exception;

/**
 * Exception thrown when alert configuration is invalid.
 */
public class InvalidAlertConfigurationException extends PriceAlertException {

    public static final String ERROR_CODE = "INVALID_ALERT_CONFIG";

    public InvalidAlertConfigurationException(String message) {
        super(message, ERROR_CODE);
    }
}
