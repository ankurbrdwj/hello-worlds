package com.ankur.candlesticks.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.entity.Quote;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@EnableJpaAuditing
public class QuoteRepositoryTest {

  @Autowired
  private QuoteRepository quoteRepository;

  @Autowired
  private InstrumentsRepository instrumentRepository;

  private Instrument instrument;
  private Instant testStartTime;

  @BeforeEach
  void setup() {
    testStartTime = Instant.now();

    // Create an Instrument and save it to the database
    instrument = Instrument.builder()
        .setIsin("TEST_ISIN_123")
        .setDescription("Test Instrument")
        .setActive(true)
        .build();
    instrumentRepository.save(instrument);

    // Add some test quotes for the instrument
    // Note: @CreatedDate will set createdTime to NOW for all quotes
    Quote quote1 = Quote.builder()
        .setInstrument(instrument)
        .setPrice(100.0)
        .build();
    quoteRepository.save(quote1);

    Quote quote2 = Quote.builder()
        .setInstrument(instrument)
        .setPrice(110.0)
        .build();
    quoteRepository.save(quote2);

    Quote quote3 = Quote.builder()
        .setInstrument(instrument)
        .setPrice(105.0)
        .build();
    quoteRepository.save(quote3);
  }

  @Test
  void testFindQuotesForInstrumentInTimePeriod() {
    // All quotes were created during test setup (approximately now)
    Instant endTime = Instant.now().plusSeconds(60); // buffer for test execution
    Instant startTime = testStartTime.minusSeconds(60); // buffer before test started

    // Call the repository method
    List<Quote> quotes = quoteRepository.findQuotesForInstrumentInTimePeriod(
        instrument.getIsin(), startTime, endTime);

    // Assert all 3 quotes are returned (created during test setup)
    assertThat(quotes).hasSize(3);
  }

  @Test
  void testFindQuotesForInstrumentInTimePeriod_NoResults() {
    // Query for time period in the past (before any quotes existed)
    Instant endTime = testStartTime.minusSeconds(3600); // 1 hour before test
    Instant startTime = endTime.minusSeconds(1800); // 30 minutes before that

    // Call the repository method
    List<Quote> quotes = quoteRepository.findQuotesForInstrumentInTimePeriod(
        instrument.getIsin(), startTime, endTime);

    // Assert no quotes returned
    assertThat(quotes).isEmpty();
  }

  @Test
  void testFindQuotesForInstrumentInTimePeriod_DifferentIsin() {
    // Query with wrong ISIN
    Instant endTime = Instant.now().plusSeconds(60);
    Instant startTime = testStartTime.minusSeconds(60);

    List<Quote> quotes = quoteRepository.findQuotesForInstrumentInTimePeriod(
        "WRONG_ISIN", startTime, endTime);

    // Assert no quotes returned for non-existent ISIN
    assertThat(quotes).isEmpty();
  }
}
