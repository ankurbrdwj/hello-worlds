package com.ankur.exchange.feed.websocket;

import com.ankur.exchange.feed.model.MessageType;
import com.ankur.exchange.feed.model.Quote;
import com.ankur.exchange.feed.model.WebsocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuotesWebSocketHandlerTest {

    private QuotesWebSocketHandler handler;
    private WebSocketSession mockSession;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new QuotesWebSocketHandler();
        mockSession = mock(WebSocketSession.class);
        objectMapper = new ObjectMapper();
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getId()).thenReturn("test-session-123");
    }

    @Test
    void testConnectionEstablished() throws Exception {
        // When
        handler.afterConnectionEstablished(mockSession);

        // Then
        verify(mockSession, atLeastOnce()).getId();
    }

    @Test
    void testConnectionClosed() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);

        // When
        handler.afterConnectionClosed(mockSession, null);

        // Then
        verify(mockSession, atLeastOnce()).getId();
    }

    @Test
    void testSendPrice() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);
        Quote quote = new Quote("AAPL", new BigDecimal("150.25"));

        // When
        handler.sendPrice(quote);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession).sendMessage(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue().getPayload();
        WebsocketMessage<?> parsedMessage = objectMapper.readValue(sentMessage, WebsocketMessage.class);

        assertEquals(MessageType.QUOTE, parsedMessage.getType());
        assertNotNull(parsedMessage.getData());

        System.out.println("✓ Send price test passed");
        System.out.println("  Sent message: " + sentMessage);
    }

    @Test
    void testMultipleQuotes() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);
        Quote quote1 = new Quote("AAPL", new BigDecimal("150.25"));
        Quote quote2 = new Quote("GOOGL", new BigDecimal("2800.50"));
        Quote quote3 = new Quote("MSFT", new BigDecimal("380.75"));

        // When
        handler.sendPrice(quote1);
        handler.sendPrice(quote2);
        handler.sendPrice(quote3);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, times(3)).sendMessage(messageCaptor.capture());

        assertEquals(3, messageCaptor.getAllValues().size());

        System.out.println("✓ Multiple quotes test passed");
        System.out.println("  Sent 3 quotes successfully");
    }

    @Test
    void testMultipleSessionsReceiveQuotes() throws Exception {
        // Given
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);

        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session3.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");
        when(session3.getId()).thenReturn("session-3");

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionEstablished(session3);

        Quote quote = new Quote("TSLA", new BigDecimal("720.50"));

        // When
        handler.sendPrice(quote);

        // Then - All sessions should receive the quote
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
        verify(session3).sendMessage(any(TextMessage.class));

        System.out.println("✓ Multiple sessions receive quotes test passed");
        System.out.println("  3 sessions received the quote");
    }

    @Test
    void testClosedSessionDoesNotReceiveQuotes() throws Exception {
        // Given
        WebSocketSession openSession = mock(WebSocketSession.class);
        WebSocketSession closedSession = mock(WebSocketSession.class);

        when(openSession.isOpen()).thenReturn(true);
        when(closedSession.isOpen()).thenReturn(false);
        when(openSession.getId()).thenReturn("open-session");
        when(closedSession.getId()).thenReturn("closed-session");

        handler.afterConnectionEstablished(openSession);
        handler.afterConnectionEstablished(closedSession);

        Quote quote = new Quote("NVDA", new BigDecimal("500.00"));

        // When
        handler.sendPrice(quote);

        // Then
        verify(openSession).sendMessage(any(TextMessage.class));
        verify(closedSession, never()).sendMessage(any(TextMessage.class));

        System.out.println("✓ Closed session does not receive quotes test passed");
    }

    @Test
    void testQuoteWithDifferentPrices() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);

        // When
        handler.sendPrice(new Quote("AAPL", new BigDecimal("100.00")));
        handler.sendPrice(new Quote("AAPL", new BigDecimal("100.50")));
        handler.sendPrice(new Quote("AAPL", new BigDecimal("99.75")));

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession, times(3)).sendMessage(messageCaptor.capture());

        for (TextMessage message : messageCaptor.getAllValues()) {
            String payload = message.getPayload();
            assertTrue(payload.contains("AAPL"));
            assertTrue(payload.contains("QUOTE"));
        }

        System.out.println("✓ Quote with different prices test passed");
        System.out.println("  Price updates sent successfully");
    }

    @Test
    void testQuoteMessageStructure() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);
        String isin = "TEST123";
        BigDecimal price = new BigDecimal("42.42");
        Quote quote = new Quote(isin, price);

        // When
        handler.sendPrice(quote);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession).sendMessage(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue().getPayload();

        // Verify JSON structure
        assertTrue(sentMessage.contains("\"type\""), "Message should contain type field");
        assertTrue(sentMessage.contains("\"QUOTE\""), "Message should contain QUOTE type");
        assertTrue(sentMessage.contains("\"data\""), "Message should contain data field");
        assertTrue(sentMessage.contains(isin), "Message should contain ISIN");
        assertTrue(sentMessage.contains(price.toString()), "Message should contain price");

        System.out.println("✓ Quote message structure test passed");
        System.out.println("  Message structure verified: " + sentMessage);
    }
}