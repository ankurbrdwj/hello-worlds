package com.ankur.ratelimiter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe Sliding Window Counter Rate Limiter with memory cleanup.
 *
 * Improvements:
 * 1. Thread-safe: synchronized access to WindowData
 * 2. Memory cleanup: tracks lastAccessTime, can evict old entries
 * 3. Integer math where possible to avoid floating point issues
 */
public class CounterLimiterImpl implements RateLimiter {

    private final int maxRequests;
    private final long windowSizeMillis;

    // Store window data per key
    private final Map<String, WindowData> keyData;

    public CounterLimiterImpl(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.keyData = new ConcurrentHashMap<>();
    }

    @Override
    public boolean allow(String key, long nowMillis) {
        // Calculate which window we're in
        long currentWindowId = nowMillis / windowSizeMillis;

        // Get or create data for this key
        WindowData data = keyData.computeIfAbsent(key, k -> new WindowData());

        // Synchronize on the specific WindowData object for thread safety
        // This allows different keys to be processed concurrently
        synchronized (data) {
            // Update last access time for cleanup
            data.lastAccessTime = nowMillis;

            // If we're in a new window, shift the data
            if (currentWindowId > data.currentWindowId) {
                data.previousWindowCount = data.currentWindowCount;
                data.previousWindowId = data.currentWindowId;
                data.currentWindowCount = 0;
                data.currentWindowId = currentWindowId;
            }

            // Calculate how far we are into the current window
            // Example: windowSizeMillis = 60_000 (1 min), nowMillis = 125_000
            //   -> currentWindowId = 125_000 / 60_000 = 2 (windows: [0,60k), [60k,120k), [120k,180k)...)
            //   -> timeIntoCurrentWindow = 125_000 % 60_000 = 5_000ms (5s into window #2)
            long timeIntoCurrentWindow = nowMillis % windowSizeMillis;

            // Calculate overlap with previous window (as a percentage)
            // Continuing example: overlap = (60_000 - 5_000) / 60_000 = 0.9166 (91.66%)
            //   -> we're near the start of the window, so most of the previous window still "counts"
            // Edge cases:
            //   timeIntoCurrentWindow = 0       -> overlap = 1.0  (acts like a fixed window, full previous count applied)
            //   timeIntoCurrentWindow = 59_999  -> overlap ≈ 0.0  (previous window barely counts anymore)
            double previousWindowOverlap = (windowSizeMillis - timeIntoCurrentWindow) / (double) windowSizeMillis;  // get previous overlap

            // Calculate weighted count
            // Example: previousWindowCount = 80, currentWindowCount = 20, overlap = 0.9166
            //   -> weightedCount = (80 * 0.9166) + 20 = 73.33 + 20 = 93.33
            //   -> with maxRequests = 100, 93.33 < 100 so the request is ALLOWED (barely)
            // Another example, later in the window: overlap = 0.1, same counts
            //   -> weightedCount = (80 * 0.1) + 20 = 8 + 20 = 28  -> well under limit, plenty of room
            double weightedCount = (data.previousWindowCount * previousWindowOverlap) + data.currentWindowCount; //

            // Check if we're under the limit
            if (weightedCount < maxRequests) {
                data.currentWindowCount++;
                return true;
            }

            return false;
        }
    }

    /**
     * Remove entries that haven't been accessed for a while to prevent memory leaks.
     * Call this periodically (e.g., every minute) in production.
     *
     * @param nowMillis Current time in milliseconds
     * @param maxIdleTimeMillis Remove entries idle longer than this (e.g., 5 minutes)
     */
    public void cleanup(long nowMillis, long maxIdleTimeMillis) {
        keyData.entrySet().removeIf(entry -> {
            WindowData data = entry.getValue();
            synchronized (data) {
                return (nowMillis - data.lastAccessTime) > maxIdleTimeMillis;
            }
        });
    }

    /**
     * Get current number of tracked keys (for monitoring).
     */
    public int getTrackedKeyCount() {
        return keyData.size();
    }

    // Helper class to store window data per key
    private static class WindowData {
        long currentWindowId = 0;
        int currentWindowCount = 0;
        long previousWindowId = -1;
        int previousWindowCount = 0;
        volatile long lastAccessTime = 0;  // For cleanup tracking
    }
}