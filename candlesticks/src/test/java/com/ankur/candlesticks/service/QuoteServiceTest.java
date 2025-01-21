package com.ankur.candlesticks.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ankur.candlesticks.repository.InstrumentsRepository;
import com.ankur.candlesticks.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.socket.client.WebSocketClient;

public class QuoteServiceTest {

  @InjectMocks
  private QuoteService quoteService;

  @Mock
  private CandlestickService candlestickService;
  @Mock
  QuoteRepository qouteRepository;
  @Mock
  InstrumentsRepository instrumentRepository;

  @BeforeEach
  void setUp(){
    quoteService= new QuoteServiceImpl(qouteRepository,instrumentRepository);
  }
    @Test
    public void testProcessQuote() {
      quoteService.processQuote("ABC123", 100.0);

      verify(candlestickService, times(1)).updateCandlestick("ABC123", 100.0);
    }
  }
