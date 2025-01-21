package com.ankur.candlesticks.service;

import com.ankur.candlesticks.entity.Instrument;
import java.util.List;

public interface InstrumentService {
  void deleteInstrument(String abc123);
  boolean instrumentExists(String isin);
  void addInstrument(String isin, String instrumentName);

  List<Instrument> getInstrumentsFromLastMinutes(int minutes);
}
