package com.ankur.price_alert.model;

public class PriceUpdate {
    private final String symbol;
    private final double price;
    private final long timestamp;

    public PriceUpdate(String symbol, double price) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    public PriceUpdate(String symbol, double price, long timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
}

