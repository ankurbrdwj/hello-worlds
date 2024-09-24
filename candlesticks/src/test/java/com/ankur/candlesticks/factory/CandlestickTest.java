package com.ankur.candlesticks.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class CandlestickTest {

    @Test
    public void testCandlestickCreation() {
      LocalDateTime timestamp = LocalDateTime.of(2019, 3, 5, 13, 0, 0);
      Candlestick candlestick = new Candlestick(timestamp, 10.0, 15.0, 10.0, 12.0, timestamp.plusMinutes(1));

      assertEquals(10.0, candlestick.getOpenPrice());
      assertEquals(15.0, candlestick.getHighPrice());
      assertEquals(12.0, candlestick.getClosePrice());
      assertEquals(10.0, candlestick.getLowPrice());
    }
  }

