package com.ankur.candlesticks.service;

import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.repository.InstrumentsRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentServiceImpl implements InstrumentService {

  private final InstrumentsRepository instrumentsRepository;

  private final Map<String, String> instruments = new ConcurrentHashMap<>();

  @Override
  public void addInstrument(String isin, String description) {
    Instrument instrument= Instrument.builder()
      .setIsin(isin)
          .setDescription(description)
            .build();
    instrumentsRepository.save(instrument);
    instruments.put(isin, description);
  }

  @Override
  public List<Instrument> getInstrumentsFromLastMinutes(int minutes) {
    return List.of();
  }

  @Override
  public void deleteInstrument(String isin) {
    instruments.remove(isin);
  }

  public boolean instrumentExists(String isin) {
    return instruments.containsKey(isin);
  }
}
