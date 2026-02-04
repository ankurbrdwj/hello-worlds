package com.ankur.exchange.feed.websocket;

import com.ankur.exchange.feed.model.MessageType;
import com.ankur.exchange.feed.model.Quote;
import com.ankur.exchange.feed.model.WebsocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class QuotesWebSocketHandler extends TextWebSocketHandler {
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("New quotes WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("Quotes WebSocket connection closed: {}", session.getId());
    }

    public void sendPrice(Quote quote) {
        WebsocketMessage<Quote> message = new WebsocketMessage<>(MessageType.QUOTE, quote);
        String json = serializeMessage(message);

        for (WebSocketSession session : sessions) {
            sendMessage(session, json);
        }
    }

    private void sendMessage(WebSocketSession session, String json) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private String serializeMessage(WebsocketMessage<Quote> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error serializing message: {}", e.getMessage());
            return "{}";
        }
    }
}