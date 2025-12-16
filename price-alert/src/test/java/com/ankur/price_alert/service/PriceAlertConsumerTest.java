package com.ankur.price_alert.service;

import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.*;

public class PriceAlertConsumerTest {
    private PriceAlertConsumer priceAlertConsumer;
    private PriceAlertService priceAlertService;

    @BeforeEach
    void setUp() {
        priceAlertService = mock(PriceAlertService.class); // mock AlertEvaluator
        priceAlertConsumer = new PriceAlertConsumer(priceAlertService);
    }



}
