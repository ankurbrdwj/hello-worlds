package com.ankur.candlesticks.service;

import com.ankur.candlesticks.dto.Candlestick;
import com.ankur.candlesticks.entity.Quote;
import java.util.List;

public interface CandlestickService {

  /**
   * Build candlesticks from a list of Quote entities.
   * Groups quotes by minute and creates OHLC candlesticks.
   *
   * @param quotes List of Quote entities (from database)
   * @return List of Candlesticks grouped by minute
   */
  List<Candlestick> buildCandlesticksFromQuotes(List<Quote> quotes);

  /**
   * Get candlesticks for an instrument from the last N minutes.
   * Fetches quotes from DB and converts to candlesticks.
   */
  List<Candlestick> getCandlesticks(String isin, int minutes);
}
