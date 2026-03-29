package com.ankur.bdd.exception;

public abstract class PriceAlertException extends RuntimeException {

    private final String errorCode;

    protected PriceAlertException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}