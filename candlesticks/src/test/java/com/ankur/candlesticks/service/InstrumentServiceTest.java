package com.ankur.candlesticks.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class InstrumentServiceTest {

    @BeforeEach
    void setUp(){
      instrumentService= new InstrumentServiceImpl();
    }
    @InjectMocks
    private InstrumentService instrumentService;

    @Test
    public void testAddInstrument() {
      instrumentService.addInstrument("ABC123", "Test Instrument");

      assertTrue(instrumentService.instrumentExists("ABC123"));
    }

    @Test
    public void testDeleteInstrument() {
      instrumentService.addInstrument("ABC123", "Test Instrument");
      instrumentService.deleteInstrument("ABC123");

      assertFalse(instrumentService.instrumentExists("ABC123"));
    }
  }
