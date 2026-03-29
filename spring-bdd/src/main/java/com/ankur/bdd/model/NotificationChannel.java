package com.ankur.bdd.model;

public enum NotificationChannel {
    EMAIL("Email"),
    SMS("SMS"),
    PUSH("Push Notification"),
    WEBHOOK("Webhook");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}