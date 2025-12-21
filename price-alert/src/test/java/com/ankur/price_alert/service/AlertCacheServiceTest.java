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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertCacheServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;

    private AlertCacheService alertCacheService;

    private User testUser;

    @BeforeEach
    void setUp() {
        alertCacheService = new AlertCacheService(alertRepository);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Test
    void getAlertIndexForSymbol_FirstCall_ShouldQueryDatabase() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When
        alertCacheService.getAlertIndexForSymbol("TCS");

        // Then
        verify(alertRepository).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
    }

    @Test
    void getAlertIndexForSymbol_SecondCall_ShouldUseCacheNotDatabase() {
        // Given
        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When - two calls
        alertCacheService.getAlertIndexForSymbol("TCS");
        alertCacheService.getAlertIndexForSymbol("TCS");

        // Then - DB should be called only once (second call uses cache)
        verify(alertRepository, times(1)).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
    }

    @Test
    void getAlertIndexForSymbol_DifferentSymbols_ShouldQueryDatabaseForEach() {
        // Given
        when(alertRepository.findBySymbolAndStatus(anyString(), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        // When
        alertCacheService.getAlertIndexForSymbol("TCS");
        alertCacheService.getAlertIndexForSymbol("INFY");

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
        alertCacheService.getAlertIndexForSymbol("TCS");

        // Invalidate cache
        alertCacheService.invalidateCacheForSymbol("TCS");

        // Second call - should query DB again
        alertCacheService.getAlertIndexForSymbol("TCS");

        // Then - DB should be called twice
        verify(alertRepository, times(2)).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
    }

    @Test
    void invalidateAllCache_ShouldForceDbQueryForAllSymbols() {
        // Given
        when(alertRepository.findBySymbolAndStatus(anyString(), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        // First calls - loads cache
        alertCacheService.getAlertIndexForSymbol("TCS");
        alertCacheService.getAlertIndexForSymbol("INFY");

        // Invalidate all cache
        alertCacheService.invalidateAllCache();

        // Second calls - should query DB again
        alertCacheService.getAlertIndexForSymbol("TCS");
        alertCacheService.getAlertIndexForSymbol("INFY");

        // Then - DB should be called 4 times (2 initial + 2 after invalidate)
        verify(alertRepository, times(2)).findBySymbolAndStatus("TCS", AlertStatus.ACTIVE);
        verify(alertRepository, times(2)).findBySymbolAndStatus("INFY", AlertStatus.ACTIVE);
    }

    @Test
    void getCacheStats_ShouldReturnCorrectStats() {
        // Given
        when(alertRepository.findBySymbolAndStatus(anyString(), eq(AlertStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        alertCacheService.getAlertIndexForSymbol("TCS");
        alertCacheService.getAlertIndexForSymbol("INFY");

        // When
        Map<String, Object> stats = alertCacheService.getCacheStats();

        // Then
        assertEquals(2, stats.get("cachedSymbols"));
        assertTrue(((java.util.Set<?>) stats.get("symbols")).contains("TCS"));
        assertTrue(((java.util.Set<?>) stats.get("symbols")).contains("INFY"));
    }

    @Test
    void getAlertIndexForSymbol_WithAlerts_ShouldBuildCorrectIndex() {
        // Given
        PriceAlert aboveAlert = createAlert(AlertType.PRICE_ABOVE, 400.0);
        PriceAlert belowAlert = createAlert(AlertType.PRICE_BELOW, 500.0);

        when(alertRepository.findBySymbolAndStatus("TCS", AlertStatus.ACTIVE))
                .thenReturn(Arrays.asList(aboveAlert, belowAlert));

        // When
        AlertCacheService.CachedAlertIndex index = alertCacheService.getAlertIndexForSymbol("TCS");

        // Then
        assertFalse(index.isEmpty());
        assertEquals(2, index.getTotalAlertCount());
        assertFalse(index.getPriceAboveAlerts().isEmpty());
        assertFalse(index.getPriceBelowAlerts().isEmpty());
    }

    @Test
    void cachedAlertIndex_IsEmpty_ShouldReturnTrueForEmptyList() {
        // Given
        AlertCacheService.CachedAlertIndex index = new AlertCacheService.CachedAlertIndex(Collections.emptyList());

        // Then
        assertTrue(index.isEmpty());
        assertEquals(0, index.getTotalAlertCount());
    }

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
