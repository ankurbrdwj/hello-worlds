package com.ankur.price_alert.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PriceFeedSimulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Stock symbols with their base prices
    private final Map<String, Double> stocks = new ConcurrentHashMap<>(Map.of(
            "TCS", 3500.0,
            "INFY", 1450.0,
            "RELIANCE", 2400.0,
            "HDFC", 1600.0,
            "ICICIBANK", 950.0
    ));

    // Current prices (will fluctuate)
    private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();

    @Autowired
    public PriceFeedSimulator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        // Initialize current prices to base prices
        currentPrices.putAll(stocks);
    }

    /**
     * Generates a realistic price movement (-2% to +2%)
     */
    private double generatePriceChange(double currentPrice) {
        double changePercent = (random.nextDouble() - 0.5) * 0.04; // -2% to +2%
        return currentPrice * (1 + changePercent);
    }

    /**
     * Sends a single price update for a random stock
     */
    public void sendPriceUpdate() {
        String[] symbols = stocks.keySet().toArray(new String[0]);
        String symbol = symbols[random.nextInt(symbols.length)];

        double newPrice = generatePriceChange(currentPrices.get(symbol));
        currentPrices.put(symbol, newPrice);

        // Send just the price (current consumer expects this)
        String message = String.format("%.2f", newPrice);
        kafkaTemplate.send("price_updates", symbol, message);

        System.out.println("📤 Sent: " + symbol + " = " + message);
    }

    /**
     * Sends price update for a specific symbol
     */
    public void sendPriceUpdate(String symbol, double price) {
        String message = String.format("%.2f", price);
        kafkaTemplate.send("price_updates", symbol, message);
    }

    /**
     * Burst mode: sends N messages as fast as possible
     */
    public void sendBurst(int count) {
        System.out.println("🚀 Starting burst of " + count + " messages...");
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            sendPriceUpdate();
        }

        long duration = System.currentTimeMillis() - start;
        System.out.println("✅ Burst complete: " + count + " messages in " + duration + "ms ("
                + (count * 1000 / Math.max(duration, 1)) + " msg/sec)");
    }

    /**
     * Load test: sends messages at specified rate for duration
     */
    public void loadTest(int messagesPerSecond, int durationSeconds) {
        System.out.println("🔥 Starting load test: " + messagesPerSecond + " msg/sec for " + durationSeconds + "s");

        long intervalNanos = 1_000_000_000L / messagesPerSecond;
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        int sent = 0;

        while (System.currentTimeMillis() < endTime) {
            long start = System.nanoTime();
            sendPriceUpdate();
            sent++;

            // Precise timing
            long elapsed = System.nanoTime() - start;
            long sleepNanos = intervalNanos - elapsed;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println("✅ Load test complete: " + sent + " messages sent");
    }

    /**
     * Start continuous feed (1 message per second)
     */
    public void startContinuousFeed() {
        running.set(true);
        System.out.println("▶️ Continuous feed started");
    }

    /**
     * Stop continuous feed
     */
    public void stopContinuousFeed() {
        running.set(false);
        System.out.println("⏹️ Continuous feed stopped");
    }

    @Scheduled(fixedRate = 1000)
    public void scheduledFeed() {
        if (running.get()) {
            sendPriceUpdate();
        }
    }

    public Map<String, Double> getCurrentPrices() {
        return new ConcurrentHashMap<>(currentPrices);
    }
}