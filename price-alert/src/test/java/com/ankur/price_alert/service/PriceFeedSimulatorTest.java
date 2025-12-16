package com.ankur.price_alert.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceFeedSimulatorTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private PriceFeedSimulator simulator;

    private static final Set<String> EXPECTED_SYMBOLS = Set.of("TCS", "INFY", "RELIANCE", "HDFC", "ICICIBANK");

    @BeforeEach
    void setUp() {
        simulator = new PriceFeedSimulator(kafkaTemplate);
    }

    // ==================== getCurrentPrices Tests ====================

    @Test
    void getCurrentPrices_shouldReturnAllStocks() {
        // When
        Map<String, Double> prices = simulator.getCurrentPrices();

        // Then
        assertEquals(5, prices.size());
        assertTrue(prices.containsKey("TCS"));
        assertTrue(prices.containsKey("INFY"));
        assertTrue(prices.containsKey("RELIANCE"));
        assertTrue(prices.containsKey("HDFC"));
        assertTrue(prices.containsKey("ICICIBANK"));
    }

    @Test
    void getCurrentPrices_shouldReturnInitialBasePrices() {
        // When
        Map<String, Double> prices = simulator.getCurrentPrices();

        // Then
        assertEquals(3500.0, prices.get("TCS"));
        assertEquals(1450.0, prices.get("INFY"));
        assertEquals(2400.0, prices.get("RELIANCE"));
        assertEquals(1600.0, prices.get("HDFC"));
        assertEquals(950.0, prices.get("ICICIBANK"));
    }

    @Test
    void getCurrentPrices_shouldReturnDefensiveCopy() {
        // When
        Map<String, Double> prices1 = simulator.getCurrentPrices();
        prices1.put("TCS", 9999.0); // Modify returned map
        Map<String, Double> prices2 = simulator.getCurrentPrices();

        // Then - original should be unchanged
        assertEquals(3500.0, prices2.get("TCS"));
    }

    // ==================== sendPriceUpdate() Tests ====================

    @Test
    void sendPriceUpdate_shouldSendToKafka() {
        // When
        simulator.sendPriceUpdate();

        // Then
        verify(kafkaTemplate).send(eq("price_updates"), anyString(), anyString());
    }

    @Test
    void sendPriceUpdate_shouldSendValidSymbol() {
        // Given
        ArgumentCaptor<String> symbolCaptor = ArgumentCaptor.forClass(String.class);

        // When
        simulator.sendPriceUpdate();

        // Then
        verify(kafkaTemplate).send(eq("price_updates"), symbolCaptor.capture(), anyString());
        assertTrue(EXPECTED_SYMBOLS.contains(symbolCaptor.getValue()));
    }

    @Test
    void sendPriceUpdate_shouldSendFormattedPrice() {
        // Given
        ArgumentCaptor<String> priceCaptor = ArgumentCaptor.forClass(String.class);

        // When
        simulator.sendPriceUpdate();

        // Then
        verify(kafkaTemplate).send(eq("price_updates"), anyString(), priceCaptor.capture());
        String priceStr = priceCaptor.getValue();

        // Should be a valid double format with 2 decimal places
        assertDoesNotThrow(() -> Double.parseDouble(priceStr));
        assertTrue(priceStr.matches("\\d+\\.\\d{2}"));
    }

    @Test
    void sendPriceUpdate_shouldUpdateCurrentPrice() {
        // Given
        Map<String, Double> initialPrices = simulator.getCurrentPrices();

        // When - send multiple updates to ensure at least one stock changes
        for (int i = 0; i < 100; i++) {
            simulator.sendPriceUpdate();
        }
        Map<String, Double> updatedPrices = simulator.getCurrentPrices();

        // Then - at least one price should have changed
        boolean anyChanged = false;
        for (String symbol : EXPECTED_SYMBOLS) {
            if (!initialPrices.get(symbol).equals(updatedPrices.get(symbol))) {
                anyChanged = true;
                break;
            }
        }
        assertTrue(anyChanged, "At least one price should have changed after 100 updates");
    }

    @Test
    void sendPriceUpdate_priceChangeShouldBeWithinRange() {
        // Given - TCS base price is 3500
        double basePrice = 3500.0;
        double minExpected = basePrice * 0.98; // -2%
        double maxExpected = basePrice * 1.02; // +2%

        // When
        simulator.sendPriceUpdate();
        Map<String, Double> prices = simulator.getCurrentPrices();

        // Then - all prices should be within reasonable range of base
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            double price = entry.getValue();
            double originalBase = getBasePrice(entry.getKey());
            // After one update, price should be within 2% of base
            assertTrue(price >= originalBase * 0.98 && price <= originalBase * 1.02,
                    "Price for " + entry.getKey() + " should be within 2% of base");
        }
    }

    // ==================== sendPriceUpdate(symbol, price) Tests ====================

    @Test
    void sendPriceUpdate_withSymbolAndPrice_shouldSendToKafka() {
        // When
        simulator.sendPriceUpdate("TCS", 3600.50);

        // Then
        verify(kafkaTemplate).send("price_updates", "TCS", "3600.50");
    }

    @Test
    void sendPriceUpdate_withSymbolAndPrice_shouldFormatToTwoDecimals() {
        // When
        simulator.sendPriceUpdate("INFY", 1455.123456);

        // Then
        verify(kafkaTemplate).send("price_updates", "INFY", "1455.12");
    }

    @Test
    void sendPriceUpdate_withCustomSymbol_shouldSendToKafka() {
        // When
        simulator.sendPriceUpdate("AAPL", 150.00);

        // Then
        verify(kafkaTemplate).send("price_updates", "AAPL", "150.00");
    }

    // ==================== sendBurst Tests ====================

    @Test
    void sendBurst_shouldSendExactCount() {
        // When
        simulator.sendBurst(100);

        // Then
        verify(kafkaTemplate, times(100)).send(eq("price_updates"), anyString(), anyString());
    }

    @Test
    void sendBurst_withZeroCount_shouldSendNothing() {
        // When
        simulator.sendBurst(0);

        // Then
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendBurst_shouldSendValidSymbols() {
        // Given
        ArgumentCaptor<String> symbolCaptor = ArgumentCaptor.forClass(String.class);

        // When
        simulator.sendBurst(50);

        // Then
        verify(kafkaTemplate, times(50)).send(eq("price_updates"), symbolCaptor.capture(), anyString());
        for (String symbol : symbolCaptor.getAllValues()) {
            assertTrue(EXPECTED_SYMBOLS.contains(symbol), "Symbol " + symbol + " should be valid");
        }
    }

    // ==================== Continuous Feed Tests ====================

    @Test
    void startContinuousFeed_shouldEnableFeed() {
        // When
        simulator.startContinuousFeed();
        simulator.scheduledFeed();

        // Then - should send message when running
        verify(kafkaTemplate).send(eq("price_updates"), anyString(), anyString());
    }

    @Test
    void stopContinuousFeed_shouldDisableFeed() {
        // Given
        simulator.startContinuousFeed();
        simulator.stopContinuousFeed();

        // When
        simulator.scheduledFeed();

        // Then - should not send when stopped
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void scheduledFeed_whenNotRunning_shouldNotSend() {
        // When - feed not started
        simulator.scheduledFeed();

        // Then
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void continuousFeed_toggleOnOff_shouldWorkCorrectly() {
        // Start -> should send
        simulator.startContinuousFeed();
        simulator.scheduledFeed();
        verify(kafkaTemplate, times(1)).send(eq("price_updates"), anyString(), anyString());

        // Stop -> should not send more
        simulator.stopContinuousFeed();
        simulator.scheduledFeed();
        verify(kafkaTemplate, times(1)).send(eq("price_updates"), anyString(), anyString());

        // Start again -> should send
        simulator.startContinuousFeed();
        simulator.scheduledFeed();
        verify(kafkaTemplate, times(2)).send(eq("price_updates"), anyString(), anyString());
    }

    // ==================== Load Test ====================

    @Test
    void loadTest_shouldSendMessagesAtRate() {
        // When - 10 messages per second for 1 second
        simulator.loadTest(10, 1);

        // Then - should have sent approximately 10 messages (allow some variance)
        verify(kafkaTemplate, atLeast(8)).send(eq("price_updates"), anyString(), anyString());
        verify(kafkaTemplate, atMost(15)).send(eq("price_updates"), anyString(), anyString());
    }

    // ==================== Helper Methods ====================

    private double getBasePrice(String symbol) {
        return switch (symbol) {
            case "TCS" -> 3500.0;
            case "INFY" -> 1450.0;
            case "RELIANCE" -> 2400.0;
            case "HDFC" -> 1600.0;
            case "ICICIBANK" -> 950.0;
            default -> 0.0;
        };
    }
}