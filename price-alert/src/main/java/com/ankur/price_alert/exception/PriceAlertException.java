package com.ankur.price_alert.exception;

/**
 * Base exception for all price alert application exceptions.
 * Follows Single Responsibility Principle - dedicated exception hierarchy.
 */
public abstract class PriceAlertException extends RuntimeException {

    private final String errorCode;

    protected PriceAlertException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    protected PriceAlertException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
