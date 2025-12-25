package com.ankur.candlesticks.repository;

import com.ankur.candlesticks.entity.Quote;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

  @Query("SELECT q FROM Quote q WHERE q.createdTime >= :minutesAgo")
  List<Quote> findQuotesAfter(@Param("minutesAgo") Instant minutesAgo);
  // Fetch quotes for an instrument within a given time period
  @Query("SELECT q FROM Quote q WHERE q.instrument.isin = :isin AND q.createdTime >= :startTime AND q.createdTime <= :endTime ORDER BY q.createdTime ASC")
  List<Quote> findQuotesForInstrumentInTimePeriod(@Param("isin") String isin, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}

