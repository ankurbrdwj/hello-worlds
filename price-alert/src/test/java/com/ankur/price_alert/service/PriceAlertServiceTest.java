package com.ankur.price_alert.service;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.PriceUpdate;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PriceAlertServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;

    @Mock
    private EmailService emailService;
    @InjectMocks
    private PriceAlertService alertService;

    private PriceUpdate priceUpdate;

    @BeforeEach
    void setUp() {
        priceUpdate = new PriceUpdate("TCS", 3500.0);
    }

    @Test
    void evaluate_NoActiveAlerts_ShouldNotSendEmail() {
        // Given
        when(alertRepository.findBySymbolAndStatusActive("TCS"))
                .thenReturn(Collections.emptyList());
        // When
        alertService.evaluate(priceUpdate);
        // Then
        verify(emailService, never()).sendPriceAlertNotification(any(), anyDouble());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void evaluate_WithActiveAlerts_ShouldQueryRepository() {
        // Given
        when(alertRepository.findBySymbolAndStatusActive("TCS"))
                .thenReturn(Collections.emptyList());
        // When
        alertService.evaluate(priceUpdate);
        // Then
        verify(alertRepository).findBySymbolAndStatusActive("TCS");
    }

    @Test
    void evaluate_PriceAboveAlert_CurrentPriceAboveThreshold_ShouldTrigger() {
        // Given
        PriceAlert alert = createAlert(AlertType.PRICE_ABOVE, 3400.0);
        when(alertRepository.findBySymbolAndStatusActive("TCS"))
                .thenReturn(Arrays.asList(alert));
        // When - Current price (3500) > Threshold (3400)
        alertService.evaluate(priceUpdate);
        // Then
        verify(emailService).sendPriceAlertNotification(alert, 3500.0);
        verify(alertRepository).save(alert);
        assertEquals(AlertStatus.TRIGGERED, alert.getStatus());
    }
    private PriceAlert createAlert(AlertType type, double threshold) {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        PriceAlert alert = PriceAlert.builder()
                .id(1L)
                .symbol("TCS")
                .user(user)
                .alertType(type)
                .threshold(threshold)
                .status(AlertStatus.ACTIVE)
                .oneTime(true)
                .maxTriggers(1)
                .triggerCount(0)
                .build();
        return alert;
    }
}
