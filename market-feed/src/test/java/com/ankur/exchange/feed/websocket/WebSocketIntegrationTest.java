package com.ankur.exchange.feed.websocket;

import com.ankur.exchange.feed.model.Instrument;
import com.ankur.exchange.feed.model.MessageType;
import com.ankur.exchange.feed.model.Quote;
import com.ankur.exchange.feed.model.WebsocketMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private String instrumentsWsUrl;
    private String quotesWsUrl;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        instrumentsWsUrl = "ws://localhost:" + port + "/instruments";
        quotesWsUrl = "ws://localhost:" + port + "/quotes";
        objectMapper = new ObjectMapper();
    }

    @Test
    void testInstrumentsWebSocketConnection() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        List<WebsocketMessage<Instrument>> receivedMessages = new ArrayList<>();

        StandardWebSocketClient client = new StandardWebSocketClient();

        // When
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                String payload = message.getPayload();
                WebsocketMessage<Instrument> wsMessage = objectMapper.readValue(
                    payload,
                    new TypeReference<WebsocketMessage<Instrument>>() {}
                );
                receivedMessages.add(wsMessage);
                latch.countDown();
            }
        }, instrumentsWsUrl).get(5, TimeUnit.SECONDS);

        // Then
        boolean received = latch.await(10, TimeUnit.SECONDS);
        assertTrue(received, "Should receive at least one instrument message");
        assertFalse(receivedMessages.isEmpty(), "Should have received instruments");

        WebsocketMessage<Instrument> firstMessage = receivedMessages.get(0);
        assertNotNull(firstMessage.getType(), "Message should have a type");
        assertEquals(MessageType.ADD, firstMessage.getType(), "First messages should be ADD type");
        assertNotNull(firstMessage.getData(), "Message should contain instrument data");
        assertNotNull(firstMessage.getData().getIsin(), "Instrument should have ISIN");
        assertNotNull(firstMessage.getData().getDescription(), "Instrument should have description");

        session.close();
        System.out.println("✓ Instruments WebSocket test passed");
        System.out.println("  Received instrument: " + firstMessage.getData().getIsin()
            + " - " + firstMessage.getData().getDescription());
    }

    @Test
    void testQuotesWebSocketConnection() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        List<WebsocketMessage<Quote>> receivedQuotes = new ArrayList<>();

        StandardWebSocketClient client = new StandardWebSocketClient();

        // When
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                String payload = message.getPayload();
                WebsocketMessage<Quote> wsMessage = objectMapper.readValue(
                    payload,
                    new TypeReference<WebsocketMessage<Quote>>() {}
                );
                receivedQuotes.add(wsMessage);
                latch.countDown();
            }
        }, quotesWsUrl).get(5, TimeUnit.SECONDS);

        // Then
        boolean received = latch.await(10, TimeUnit.SECONDS);
        assertTrue(received, "Should receive at least one quote message");
        assertFalse(receivedQuotes.isEmpty(), "Should have received quotes");

        WebsocketMessage<Quote> firstQuote = receivedQuotes.get(0);
        assertNotNull(firstQuote.getType(), "Quote message should have a type");
        assertEquals(MessageType.QUOTE, firstQuote.getType(), "Message type should be QUOTE");
        assertNotNull(firstQuote.getData(), "Quote message should contain data");
        assertNotNull(firstQuote.getData().getIsin(), "Quote should have ISIN");
        assertNotNull(firstQuote.getData().getPrice(), "Quote should have price");
        assertTrue(firstQuote.getData().getPrice().compareTo(BigDecimal.ZERO) > 0,
            "Quote price should be positive");

        session.close();
        System.out.println("✓ Quotes WebSocket test passed");
        System.out.println("  Received quote: " + firstQuote.getData().getIsin()
            + " @ " + firstQuote.getData().getPrice());
    }

    @Test
    void testMultipleInstrumentMessages() throws Exception {
        // Given
        int expectedMessages = 5;
        CountDownLatch latch = new CountDownLatch(expectedMessages);
        List<WebsocketMessage<Instrument>> receivedMessages = new ArrayList<>();

        StandardWebSocketClient client = new StandardWebSocketClient();

        // When
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                String payload = message.getPayload();
                WebsocketMessage<Instrument> wsMessage = objectMapper.readValue(
                    payload,
                    new TypeReference<WebsocketMessage<Instrument>>() {}
                );
                receivedMessages.add(wsMessage);
                latch.countDown();
            }
        }, instrumentsWsUrl).get(5, TimeUnit.SECONDS);

        // Then
        boolean received = latch.await(15, TimeUnit.SECONDS);
        assertTrue(received, "Should receive multiple instrument messages");
        assertTrue(receivedMessages.size() >= expectedMessages,
            "Should have received at least " + expectedMessages + " messages");

        // Verify we have at least some ADD messages
        long addCount = receivedMessages.stream()
            .filter(msg -> msg.getType() == MessageType.ADD)
            .count();
        assertTrue(addCount > 0, "Should have received ADD messages");

        session.close();
        System.out.println("✓ Multiple instruments test passed");
        System.out.println("  Received " + receivedMessages.size() + " messages");
        System.out.println("  ADD: " + addCount + ", DELETE: " + (receivedMessages.size() - addCount));
    }

    @Test
    void testMultipleQuoteMessages() throws Exception {
        // Given
        int expectedMessages = 10;
        CountDownLatch latch = new CountDownLatch(expectedMessages);
        List<WebsocketMessage<Quote>> receivedQuotes = new ArrayList<>();

        StandardWebSocketClient client = new StandardWebSocketClient();

        // When
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                String payload = message.getPayload();
                WebsocketMessage<Quote> wsMessage = objectMapper.readValue(
                    payload,
                    new TypeReference<WebsocketMessage<Quote>>() {}
                );
                receivedQuotes.add(wsMessage);
                latch.countDown();
            }
        }, quotesWsUrl).get(5, TimeUnit.SECONDS);

        // Then
        boolean received = latch.await(15, TimeUnit.SECONDS);
        assertTrue(received, "Should receive multiple quote messages");
        assertTrue(receivedQuotes.size() >= expectedMessages,
            "Should have received at least " + expectedMessages + " quotes");

        // Verify all quotes are valid
        for (WebsocketMessage<Quote> quote : receivedQuotes) {
            assertEquals(MessageType.QUOTE, quote.getType());
            assertNotNull(quote.getData().getIsin());
            assertNotNull(quote.getData().getPrice());
            assertTrue(quote.getData().getPrice().compareTo(BigDecimal.ZERO) > 0);
        }

        session.close();
        System.out.println("✓ Multiple quotes test passed");
        System.out.println("  Received " + receivedQuotes.size() + " quote messages");
    }

    @Test
    void testBothEndpointsSimultaneously() throws Exception {
        // Given
        CountDownLatch instrumentLatch = new CountDownLatch(3);
        CountDownLatch quotesLatch = new CountDownLatch(5);
        List<WebsocketMessage<Instrument>> instruments = new ArrayList<>();
        List<WebsocketMessage<Quote>> quotes = new ArrayList<>();

        StandardWebSocketClient client = new StandardWebSocketClient();

        // When - Connect to both endpoints
        WebSocketSession instrumentSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                WebsocketMessage<Instrument> wsMessage = objectMapper.readValue(
                    message.getPayload(),
                    new TypeReference<WebsocketMessage<Instrument>>() {}
                );
                instruments.add(wsMessage);
                instrumentLatch.countDown();
            }
        }, instrumentsWsUrl).get(5, TimeUnit.SECONDS);

        WebSocketSession quotesSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                WebsocketMessage<Quote> wsMessage = objectMapper.readValue(
                    message.getPayload(),
                    new TypeReference<WebsocketMessage<Quote>>() {}
                );
                quotes.add(wsMessage);
                quotesLatch.countDown();
            }
        }, quotesWsUrl).get(5, TimeUnit.SECONDS);

        // Then
        boolean instrumentsReceived = instrumentLatch.await(15, TimeUnit.SECONDS);
        boolean quotesReceived = quotesLatch.await(15, TimeUnit.SECONDS);

        assertTrue(instrumentsReceived, "Should receive instruments");
        assertTrue(quotesReceived, "Should receive quotes");
        assertFalse(instruments.isEmpty(), "Instruments list should not be empty");
        assertFalse(quotes.isEmpty(), "Quotes list should not be empty");

        instrumentSession.close();
        quotesSession.close();

        System.out.println("✓ Both endpoints simultaneously test passed");
        System.out.println("  Instruments received: " + instruments.size());
        System.out.println("  Quotes received: " + quotes.size());
    }
}