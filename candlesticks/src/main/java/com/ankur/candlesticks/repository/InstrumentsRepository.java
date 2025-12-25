package com.ankur.candlesticks.repository;

import com.ankur.candlesticks.entity.Instrument;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstrumentsRepository extends JpaRepository<Instrument, UUID> {

  @Query("SELECT i FROM Instrument i WHERE i.createdTime >= :minutesAgo AND i.active = true")
  List<Instrument> findActiveInstrumentsAfter(@Param("minutesAgo") Instant minutesAgo);

  Instrument findByIsin(String isin);

  boolean existsByIsinAndActive(String isin, boolean active);
}
