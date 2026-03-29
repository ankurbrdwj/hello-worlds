package com.ankur.bdd.exception;

public class AlertQuotaExceededException extends PriceAlertException {

    public static final String ERROR_CODE = "ALERT_QUOTA_EXCEEDED";

    private final Long userId;
    private final int currentCount;
    private final int maxAllowed;

    public AlertQuotaExceededException(Long userId, int currentCount, int maxAllowed) {
        super(String.format("User %d has reached maximum alert limit. Current: %d, Max: %d",
                userId, currentCount, maxAllowed), ERROR_CODE);
        this.userId = userId;
        this.currentCount = currentCount;
        this.maxAllowed = maxAllowed;
    }

    public Long getUserId() { return userId; }
    public int getCurrentCount() { return currentCount; }
    public int getMaxAllowed() { return maxAllowed; }
}