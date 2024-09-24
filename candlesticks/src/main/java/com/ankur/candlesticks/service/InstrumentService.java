package com.ankur.candlesticks.service;

public interface InstrumentService {
  void deleteInstrument(String abc123);
  boolean instrumentExists(String isin);
  void addInstrument(String isin, String instrumentName);
}
