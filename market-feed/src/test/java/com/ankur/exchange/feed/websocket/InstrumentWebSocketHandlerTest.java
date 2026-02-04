package com.ankur.exchange.feed.websocket;

import com.ankur.exchange.feed.model.Instrument;
import com.ankur.exchange.feed.model.MessageType;
import com.ankur.exchange.feed.model.WebsocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstrumentWebSocketHandlerTest {

    private InstrumentWebSocketHandler handler;
    private WebSocketSession mockSession;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new InstrumentWebSocketHandler();
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
        // Session should be added to the handler's session list
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
    void testAddInstrument() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.");

        // When
        handler.addInstrument(instrument);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession).sendMessage(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue().getPayload();
        WebsocketMessage<?> parsedMessage = objectMapper.readValue(sentMessage, WebsocketMessage.class);

        assertEquals(MessageType.ADD, parsedMessage.getType());
        assertNotNull(parsedMessage.getData());

        System.out.println("✓ Add instrument test passed");
        System.out.println("  Sent message: " + sentMessage);
    }

    @Test
    void testDeleteInstrument() throws Exception {
        // Given
        handler.afterConnectionEstablished(mockSession);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.");
        handler.addInstrument(instrument);
        reset(mockSession); // Reset to clear previous interactions
        when(mockSession.isOpen()).thenReturn(true);

        // When
        handler.deleteInstrument(instrument);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(mockSession).sendMessage(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue().getPayload();
        WebsocketMessage<?> parsedMessage = objectMapper.readValue(sentMessage, WebsocketMessage.class);

        assertEquals(MessageType.DELETE, parsedMessage.getType());

        System.out.println("✓ Delete instrument test passed");
        System.out.println("  Sent message: " + sentMessage);
    }

    @Test
    void testNewConnectionReceivesExistingInstruments() throws Exception {
        // Given
        Instrument instrument1 = new Instrument("AAPL", "Apple Inc.");
        Instrument instrument2 = new Instrument("GOOGL", "Alphabet Inc.");

        WebSocketSession firstSession = mock(WebSocketSession.class);
        when(firstSession.isOpen()).thenReturn(true);
        when(firstSession.getId()).thenReturn("session-1");

        handler.afterConnectionEstablished(firstSession);
        handler.addInstrument(instrument1);
        handler.addInstrument(instrument2);

        // When - A new session connects
        WebSocketSession newSession = mock(WebSocketSession.class);
        when(newSession.isOpen()).thenReturn(true);
        when(newSession.getId()).thenReturn("session-2");

        handler.afterConnectionEstablished(newSession);

        // Then - New session should receive all existing instruments
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(newSession, atLeast(2)).sendMessage(messageCaptor.capture());

        System.out.println("✓ New connection receives existing instruments test passed");
        System.out.println("  New session received " + messageCaptor.getAllValues().size() + " instrument(s)");
    }

    @Test
    void testMultipleSessionsReceiveBroadcast() throws Exception {
        // Given
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session-1");
        when(session2.getId()).thenReturn("session-2");

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        Instrument instrument = new Instrument("MSFT", "Microsoft Corporation");

        // When
        handler.addInstrument(instrument);

        // Then - Both sessions should receive the message
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));

        System.out.println("✓ Multiple sessions broadcast test passed");
    }

    @Test
    void testClosedSessionDoesNotReceiveMessages() throws Exception {
        // Given
        WebSocketSession closedSession = mock(WebSocketSession.class);
        when(closedSession.isOpen()).thenReturn(false); // Session is closed
        when(closedSession.getId()).thenReturn("closed-session");

        handler.afterConnectionEstablished(closedSession);
        Instrument instrument = new Instrument("TSLA", "Tesla, Inc.");

        // When
        handler.addInstrument(instrument);

        // Then - Closed session should not receive the message
        verify(closedSession, never()).sendMessage(any(TextMessage.class));

        System.out.println("✓ Closed session test passed");
    }
}