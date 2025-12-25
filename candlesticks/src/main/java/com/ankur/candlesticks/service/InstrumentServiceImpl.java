package com.ankur.candlesticks.service;

import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.repository.InstrumentsRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstrumentServiceImpl implements InstrumentService {

  private final InstrumentsRepository instrumentsRepository;

  @Override
  @Transactional
  public void addInstrument(String isin, String description) {
    // Check if ISIN exists (could be inactive - ISIN reuse case)
    Instrument existing = instrumentsRepository.findByIsin(isin);

    if (existing != null) {
      // ISIN reuse: reactivate existing instrument
      log.info("Reactivating instrument: {}", isin);
      existing.setActive(true);
      existing.setDescription(description);
      existing.setDeletedAt(null);
      instrumentsRepository.save(existing);
    } else {
      // New instrument
      log.info("Adding new instrument: {}", isin);
      Instrument instrument = Instrument.builder()
          .setIsin(isin)
          .setDescription(description)
          .setActive(true)
          .build();
      instrumentsRepository.save(instrument);
    }
  }

  @Override
  public List<Instrument> getInstrumentsFromLastMinutes(int minutes) {
    Instant cutoff = Instant.now().minusSeconds(minutes * 60L);
    return instrumentsRepository.findActiveInstrumentsAfter(cutoff);
  }

  @Override
  @Transactional
  public void deleteInstrument(String isin) {
    Instrument instrument = instrumentsRepository.findByIsin(isin);
    if (instrument != null) {
      log.info("Deactivating instrument: {}", isin);
      instrument.setActive(false);
      instrument.setDeletedAt(Instant.now());
      instrumentsRepository.save(instrument);
    } else {
      log.warn("Attempted to delete non-existent instrument: {}", isin);
    }
  }

  @Override
  public boolean instrumentExists(String isin) {
    return instrumentsRepository.existsByIsinAndActive(isin, true);
  }
}
