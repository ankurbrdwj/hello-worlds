package com.ankur.candlesticks.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;



@Service
public class InstrumentServiceImpl implements InstrumentService {
  private final Map<String, String> instruments = new ConcurrentHashMap<>();

  @Override
  public void addInstrument(String isin, String description) {
    instruments.put(isin, description);
  }

  @Override
  public void deleteInstrument(String isin) {
    instruments.remove(isin);
  }

  public boolean instrumentExists(String isin) {
    return instruments.containsKey(isin);
  }
}
