package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertMatchingServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AlertCacheService alertCacheService;

    private AlertMatchingService alertMatchingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        alertMatchingService = new AlertMatchingService(alertRepository, notificationService, alertCacheService);

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
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450 > threshold 400
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService).sendPriceAlertNotification(eq(alert), eq(450.0));
        verify(alertRepository).save(alert);
        assertEquals(1, alert.getTriggerCount());
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
    }

    @Test
    void processPrice_PriceBelowThreshold_ShouldNotTriggerAboveAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 500.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450 < threshold 500
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService, never()).sendPriceAlertNotification(any(), anyDouble());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void processPrice_PriceEqualsThreshold_ShouldNotTriggerAboveAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 450.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450 == threshold 450 (not greater than)
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== PRICE_BELOW Tests ====================

    @Test
    void processPrice_PriceBelowThreshold_ShouldTriggerBelowAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BELOW, 500.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450 < threshold 500
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService).sendPriceAlertNotification(eq(alert), eq(450.0));
        verify(alertRepository).save(alert);
    }

    @Test
    void processPrice_PriceAboveThreshold_ShouldNotTriggerBelowAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BELOW, 400.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450 > threshold 400
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== PRICE_EQUALS Tests ====================

    @Test
    void processPrice_PriceWithinTolerance_ShouldTriggerEqualsAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_EQUALS, 450.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450.40 is within 0.1% of 450 (tolerance = 0.45)
        alertMatchingService.processPrice("TCS", 450.40);

        // Then
        verify(notificationService).sendPriceAlertNotification(eq(alert), eq(450.40));
    }

    @Test
    void processPrice_PriceOutsideTolerance_ShouldNotTriggerEqualsAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_EQUALS, 450.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 455 is outside 0.1% of 450 (tolerance = 0.45)
        alertMatchingService.processPrice("TCS", 455.0);

        // Then
        verify(notificationService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== PRICE_BETWEEN Tests ====================

    @Test
    void processPrice_PriceWithinRange_ShouldTriggerBetweenAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BETWEEN, 400.0);
        alert.setUpperThreshold(500.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450 is between 400 and 500
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService).sendPriceAlertNotification(eq(alert), eq(450.0));
    }

    @Test
    void processPrice_PriceOutsideRange_ShouldNotTriggerBetweenAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BETWEEN, 400.0);
        alert.setUpperThreshold(450.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 500 is outside 400-450 range
        alertMatchingService.processPrice("TCS", 500.0);

        // Then
        verify(notificationService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    @Test
    void processPrice_PriceAtLowerBound_ShouldTriggerBetweenAlert() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_BETWEEN, 400.0);
        alert.setUpperThreshold(500.0);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price at lower bound
        alertMatchingService.processPrice("TCS", 400.0);

        // Then
        verify(notificationService).sendPriceAlertNotification(eq(alert), eq(400.0));
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

        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert1, alert2, alert3));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450
        alertMatchingService.processPrice("TCS", 450.0);

        // Then - alert1 and alert2 should trigger, alert3 should not
        verify(notificationService).sendPriceAlertNotification(eq(alert1), eq(450.0));
        verify(notificationService).sendPriceAlertNotification(eq(alert2), eq(450.0));
        verify(notificationService, never()).sendPriceAlertNotification(eq(alert3), anyDouble());
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

        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(aboveAlert, belowAlert, equalsAlert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When - price 450
        alertMatchingService.processPrice("TCS", 450.0);

        // Then - all three should trigger
        verify(notificationService).sendPriceAlertNotification(eq(aboveAlert), eq(450.0));
        verify(notificationService).sendPriceAlertNotification(eq(belowAlert), eq(450.0));
        verify(notificationService).sendPriceAlertNotification(eq(equalsAlert), eq(450.0));
    }

    // ==================== One-Time Alert Tests ====================

    @Test
    void processPrice_OneTimeAlert_ShouldDeactivateAfterTrigger() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert.setOneTime(true);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        // When
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
        verify(alertRepository).save(alert);
        verify(alertCacheService).invalidateCacheForSymbol("TCS");
    }

    @Test
    void processPrice_RecurringAlert_ShouldRemainActiveAfterTrigger() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert.setOneTime(false);
        alert.setMaxTriggers(5);
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

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
        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

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
        AlertCacheService.CachedAlertIndex emptyIndex = createCacheIndex(Collections.emptyList());
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(emptyIndex);

        // When
        alertMatchingService.processPrice("TCS", 450.0);

        // Then
        verify(notificationService, never()).sendPriceAlertNotification(any(), anyDouble());
    }

    // ==================== Error Handling Tests ====================

    @Test
    void processPrice_CacheError_ShouldNotThrowException() {
        // Given
        when(alertCacheService.getAlertIndexForSymbol("TCS"))
                .thenThrow(new RuntimeException("Cache error"));

        // When/Then - should not throw
        assertDoesNotThrow(() -> alertMatchingService.processPrice("TCS", 450.0));
    }

    @Test
    void processPrice_NotificationError_ShouldContinueProcessing() {
        // Given
        PriceAlert alert1 = createAlert(AlertType.PRICE_ABOVE, 400.0);
        alert1.setId(1L);
        PriceAlert alert2 = createAlert(AlertType.PRICE_ABOVE, 420.0);
        alert2.setId(2L);

        AlertCacheService.CachedAlertIndex index = createCacheIndex(Arrays.asList(alert1, alert2));
        when(alertCacheService.getAlertIndexForSymbol("TCS")).thenReturn(index);

        doThrow(new RuntimeException("Notification error"))
                .when(notificationService).sendPriceAlertNotification(eq(alert1), anyDouble());

        // When - should not throw even if first notification fails
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

    private AlertCacheService.CachedAlertIndex createCacheIndex(List<PriceAlert> alerts) {
        return new AlertCacheService.CachedAlertIndex(alerts);
    }
}
