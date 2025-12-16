package com.ankur.price_alert.service;

import com.ankur.price_alert.dto.QuoteFeedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handles incoming WebSocket messages from the market feed (partner-service).
 * Parses quote messages and forwards to AlertMatchingService.
 */
@Component
public class MarketFeedWebSocketHandler extends TextWebSocketHandler {

    private final AlertMatchingService alertMatchingService;
    private final ObjectMapper objectMapper;

    public MarketFeedWebSocketHandler(AlertMatchingService alertMatchingService) {
        this.alertMatchingService = alertMatchingService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Connected to market feed: " + session.getUri());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            QuoteFeedMessage feedMessage = objectMapper.readValue(payload, QuoteFeedMessage.class);

            if ("QUOTE".equals(feedMessage.getType()) && feedMessage.getData() != null) {
                String symbol = feedMessage.getData().getIsin();
                double price = feedMessage.getData().getPrice();

                // Forward to alert matching service
                alertMatchingService.processPrice(symbol, price);
            }
        } catch (Exception e) {
            System.err.println("Error processing feed message: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("WebSocket transport error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("Disconnected from market feed. Status: " + status);
    }
}