package com.ankur.price_alert.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages WebSocket connection to the market feed (partner-service).
 * Handles connection, reconnection, and lifecycle.
 */
@Service
public class MarketFeedService {

    @Value("${market.feed.websocket.url:ws://localhost:9090/quotes}")
    private String feedUrl;

    @Value("${market.feed.enabled:true}")
    private boolean feedEnabled;

    @Value("${market.feed.reconnect.delay:5000}")
    private long reconnectDelayMs;

    private final MarketFeedWebSocketHandler webSocketHandler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession session;
    private volatile boolean running = false;

    public MarketFeedService(MarketFeedWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @PostConstruct
    public void init() {
        if (feedEnabled) {
            connect();
        } else {
            System.out.println("Market feed is disabled. Set market.feed.enabled=true to enable.");
        }
    }

    /**
     * Connect to the WebSocket feed
     */
    public void connect() {
        if (running) {
            System.out.println("Already connected to market feed");
            return;
        }

        try {
            System.out.println("Connecting to market feed: " + feedUrl);
            StandardWebSocketClient client = new StandardWebSocketClient();
            session = client.execute(webSocketHandler, new WebSocketHttpHeaders(), URI.create(feedUrl)).get();
            running = true;
            System.out.println("Successfully connected to market feed");
        } catch (Exception e) {
            System.err.println("Failed to connect to market feed: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Disconnect from the WebSocket feed
     */
    public void disconnect() {
        running = false;
        if (session != null && session.isOpen()) {
            try {
                session.close();
                System.out.println("Disconnected from market feed");
            } catch (Exception e) {
                System.err.println("Error closing WebSocket session: " + e.getMessage());
            }
        }
    }

    /**
     * Schedule reconnection after delay
     */
    private void scheduleReconnect() {
        if (feedEnabled) {
            System.out.println("Scheduling reconnect in " + reconnectDelayMs + "ms...");
            scheduler.schedule(this::connect, reconnectDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @PreDestroy
    public void cleanup() {
        disconnect();
        scheduler.shutdown();
    }
}