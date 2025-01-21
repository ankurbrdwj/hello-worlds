package com.ankur.candlesticks.service;

import com.ankur.candlesticks.dto.Candlestick;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CandlestickServiceImpl implements CandlestickService{

  private final Map<String, List<Candlestick>> candlesticks = new ConcurrentHashMap<>();

  public List<Candlestick> getCandlesticks(String isin) {
    return candlesticks.getOrDefault(isin, Collections.emptyList());
  }
  public void updateCandlestick(String isin, double price) {
    LocalDateTime now = LocalDateTime.now();
    List<Candlestick> candles = candlesticks.computeIfAbsent(isin, k -> new ArrayList<>());

    if (candles.isEmpty() || isNewMinute(candles.getLast(), now)) {
      Candlestick newCandle = new Candlestick();
      newCandle.setOpenTimestamp(now.truncatedTo(ChronoUnit.MINUTES));
      newCandle.setOpenPrice(price);
      newCandle.setHighPrice(price);
      newCandle.setLowPrice(price);
      newCandle.setClosePrice(price);
      newCandle.setCloseTimestamp(now);
      candles.add(newCandle);
    } else {
      Candlestick currentCandle = candles.getLast();
      currentCandle.setHighPrice(Math.max(currentCandle.getHighPrice(), price));
      currentCandle.setLowPrice(Math.min(currentCandle.getLowPrice(), price));
      currentCandle.setClosePrice(price);
      currentCandle.setCloseTimestamp(now);
    }
  }

  private boolean isNewMinute(Candlestick candle, LocalDateTime now) {
    return now.truncatedTo(ChronoUnit.MINUTES).isAfter(candle.getOpenTimestamp());
  }
}
