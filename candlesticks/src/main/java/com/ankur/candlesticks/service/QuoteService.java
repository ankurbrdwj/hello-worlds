package com.ankur.candlesticks.service;

import com.ankur.candlesticks.entity.Quote;
import java.util.List;

public interface QuoteService {
  void processQuote(String isin, double price);
  void saveQuote(String isin, double price);

  List<Quote> getQuotesFromLastMinutes(int minutes);
  List<Quote> getByIsinAndMinutes(String isisn, int minutes);
}

