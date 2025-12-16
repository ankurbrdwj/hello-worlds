package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertMatchingServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;

    @Mock
    private EmailService emailService;

    private AlertMatchingService alertMatchingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        alertMatchingService = new AlertMatchingService(alertRepository, emailService);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    // ==================== PRICE_ABOVE Tests ====================

    @Test
    void processPrice_PriceAboveThreshold_ShouldTriggerAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450 > threshold 400
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService).sendPriceAlertNotification(eq(alert), eq(450.0));
        verify(alertRepository).save(alert);
        assertEquals(1, alert.getTriggerCount());
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
    }

    @Test
    void processPrice_PriceBelowThreshold_ShouldNotTriggerAboveAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 500.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450 < threshold 500
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void processPrice_PriceEqualsThreshold_ShouldNotTriggerAboveAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 450.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450 == threshold 450 (not greater than)
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== PRICE_BELOW Tests ====================

    @Test
    void processPrice_PriceBelowThreshold_ShouldTriggerBelowAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BELOW, 500.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450 < threshold 500
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService).sendPriceAlertNotification(eq(alert), eq(450.0));
        verify(alertRepository).save(alert);
    }

    @Test
    void processPrice_PriceAboveThreshold_ShouldNotTriggerBelowAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BELOW, 400.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450 > threshold 400
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== PRICE_EQUALS Tests ====================

    @Test
    void processPrice_PriceWithinTolerance_ShouldTriggerEqualsAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_EQUALS, 450.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450.40 is within 0.1% of 450 (tolerance = 0.45)
        alertMatchingService.processPrice("TCS", 450.40);

        // Then
        verify(emailService).sendPriceAlertNotification(eq(alert), eq(450.40));
    }

    @Test
    void processPrice_PriceOutsideTolerance_ShouldNotTriggerEqualsAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_EQUALS, 450.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 455 is outside 0.1% of 450 (tolerance = 0.45)
        alertMatchingService.processPrice("TCS", 455.0);

        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== PRICE_BETWEEN Tests ====================

    @Test
    void processPrice_PriceWithinRange_ShouldTriggerBetweenAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BETWEEN, 400.0);
        alert.setUpperThreshold(500.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 450 is between 400 and 500
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService).sendPriceAlertNotification(eq(alert), eq(450.0));
    }

    @Test
    void processPrice_PriceOutsideRange_ShouldNotTriggerBetweenAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BETWEEN, 400.0);
        alert.setUpperThreshold(450.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price 500 is outside 400-450 range
        alertMatchingService.processPrice("TCS", 500.0);

        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    @Test
    void processPrice_PriceAtLowerBound_ShouldTriggerBetweenAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BETWEEN, 400.0);
        alert.setUpperThreshold(500.0);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - price at lower bound
        alertMatchingService.processPrice("TCS", 400.0);

        // Then
        verify(emailService).sendPriceAlertNotification(eq(alert), eq(400.0));
    }

    // ==================== Cache Tests ====================

    @Test
    void processPrice_FirstCall_ShouldQueryDatabase() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(alertRepository).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
    }

    @Test
    void processPrice_SecondCall_ShouldUseCacheNotDatabase() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When - two calls
        alertMatchingService.processPrice("TCS", 450.0);
        alertMatchingService.processPrice("TCS", 451.0);

        // Then - DB should be called only once (second call uses cache)
        verify(alertRepository, times(1)).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
    }

    @Test
    void processPrice_DifferentSymbols_ShouldQueryDatabaseForEach() {
        // Given
        when(alertRepository.findBySymbolAndStatus(anyString(), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        // When
        alertMatchingService.processPrice("TCS", 450.0);
        alertMatchingService.processPrice("INFY", 1500.0);

        // Then
        verify(alertRepository).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
        verify(alertRepository).findBySymbolAndStatus("INFY", AlertStatus.ACTIVE);
    }

    @Test
    void invalidateCacheForSymbol_ShouldForceDbQueryOnNextCall() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // First call - loads cache
        alertMatchingService.processPrice("TCS", 450.0);

        // Invalidate cache
        alertMatchingService.invalidateCacheForSymbol("TCS");

        // Second call - should query DB again
        alertMatchingService.processPrice("TCS", 451.0);

        // Then - DB should be called twice
        verify(alertRepository, times(2)).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
    }

    @Test
    void invalidateAllCache_ShouldForceDbQueryForAllSymbols() {
        // Given
        when(alertRepository.findBySymbolAndStatus(anyString(), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        // First calls - loads cache
        alertMatchingService.processPrice("TCS", 450.0);
        alertMatchingService.processPrice("INFY", 1500.0);

        // Invalidate all cache
        alertMatchingService.invalidateAllCache();

        // Second calls - should query DB again
        alertMatchingService.processPrice("TCS", 451.0);
        alertMatchingService.processPrice("INFY", 1501.0);

        // Then - DB should be called 4 times (2 initial + 2 after invalidate)
        verify(alertRepository, times(2)).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
        verify(alertRepository, times(2)).findBySymbolAndStatus("INFY", AlertStatus.ACTIVE);
    }

    // ==================== Multiple Alerts Tests ====================

    @Test
    void processPrice_MultipleAlerts_ShouldTriggerAllMatching() {
        // Given
        PriceAlert alert1 = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert1.setId(1L);
        PriceAlert alert2 = createAlert(AlertType.PRICE_ABOVE, 420.0);
        alert2.setId(2L);
        PriceAlert alert3 = createAlert(AlertType.PRICE_ABOVE, 500.0); // should NOT trigger
        alert3.setId(3L);

        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert1, alert2, alert3));

        // When - price 450
        alertMatchingService.processPrice("TCS", 450.0);

        // Then - alert1 and alert2 should trigger, alert3 should not
        verify(emailService).sendPriceAlertNotification(eq(alert1), eq(450.0));
        verify(emailService).sendPriceAlertNotification(eq(alert2), eq(450.0));
        verify(emailService, never()).sendPriceAlertNotification(eq(alert3), anyDouble());
    }

    @Test
    void processPrice_MixedAlertTypes_ShouldTriggerCorrectOnes() {
        // Given
        PriceAlert aboveAlert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        aboveAlert.setId(1L);
        PriceAlert belowAlert = createAlert(AlertType.PRICE_BELOW, 500.0);
        belowAlert.setId(2L);
        PriceAlert equalsAlert = createAlert(AlertType.PRICE_EQUALS, 450.0);
        equalsAlert.setId(3L);

        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(aboveAlert, belowAlert, equalsAlert));

        // When - price 450
        alertMatchingService.processPrice("TCS", 450.0);

        // Then - all three should trigger
        verify(emailService).sendPriceAlertNotification(eq(aboveAlert), eq(450.0));
        verify(emailService).sendPriceAlertNotification(eq(belowAlert), eq(450.0));
        verify(emailService).sendPriceAlertNotification(eq(equalsAlert), eq(450.0));
    }

    // ==================== One-Time Alert Tests ====================

    @Test
    void processPrice_OneTimeAlert_ShouldDeactivateAfterTrigger() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert.setOneTime(true);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
        verify(alertRepository).save(alert);
    }

    @Test
    void processPrice_RecurringAlert_ShouldRemainActiveAfterTrigger() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert.setOneTime(false);
        alert.setMaxTriggers(5);
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        assertEquals(AlertStatus.ACTIVE, alert.getStatus());
        assertEquals(1, alert.getTriggerCount());
    }

    @Test
    void processPrice_MaxTriggersReached_ShouldDeactivate() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert.setOneTime(false);
        alert.setMaxTriggers(3);
        alert.setTriggerCount(2); // Already triggered twice
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert));

        // When - third trigger
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        assertEquals(3, alert.getTriggerCount());
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
    }

    // ==================== No Alerts Tests ====================

    @Test
    void processPrice_NoAlertsForSymbol_ShouldNotSendNotification() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== Cache Stats Tests ====================

    @Test
    void getCacheStats_ShouldReturnCorrectStats() {
        // Given
        when(alertRepository.findBySymbolAndStatus(anyString(), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        alertMatchingService.processPrice("TCS", 450.0);
        alertMatchingService.processPrice("INFY", 1500.0);

        // When
        Map<String, Object> stats = alertMatchingService.getCacheStats();

        // Then
        assertEquals(2, stats.get("cachedSymbols"));
        assertTrue(((java.util.Set<?>) stats.get("symbols")).contains("TCS"));
        assertTrue(((java.util.Set<?>) stats.get("symbols")).contains("INFY"));
    }

    // ==================== Error Handling Tests ====================

    @Test
    void processPrice_DatabaseError_ShouldNotThrowException() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenThrow(new RuntimeException("Database error"));

        // When/Then - should not throw
        assertDoesNotThrow(() -> alertMatchingService.processPrice("TCS", 450.0));
    }

    @Test
    void processPrice_EmailServiceError_ShouldContinueProcessing() {
        // Given
        PriceAlert alert1 = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert1.setId(1L);
        PriceAlert alert2 = createAlert(AlertType.PRICE_ABOVE, 420.0);
        alert2.setId(2L);

        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(alert1, alert2));

        doThrow(new RuntimeException("Email error"))
                .when(emailService).sendPriceAlertNotification(eq(alert1), anyDouble());

        // When - should not throw even if first email fails
        assertDoesNotThrow(() -> alertMatchingService.processPrice("TCS", 450.0));
    }

    // ==================== Helper Methods ====================

    private PriceAlert createAlert(AlertType type, double threshold) {
        return PriceAlert.builder()
                .id(1L)
                .user(testUser)
                .symbol("TCS")
                .alertType(type)
                .threshold(threshold)
                .status(AlertStatus.ACTIVE)
                .oneTime(true)
                .maxTriggers(1)
                .triggerCount(0)
                .build();
    }
}