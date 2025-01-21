package com.ankur.candlesticks.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ankur.candlesticks.repository.InstrumentsRepository;
import com.ankur.candlesticks.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.socket.client.WebSocketClient;

public class InstrumentServiceTest {


    @InjectMocks
    private InstrumentService instrumentService;

  @Mock
  private CandlestickService candlestickService;
  @Mock
  InstrumentsRepository instrumentsRepository;
  @BeforeEach
  void setUp(){
    instrumentService= new InstrumentServiceImpl(instrumentsRepository);
  }

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
