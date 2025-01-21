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
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class QuoteRepositoryTest {

  @Autowired
  private QuoteRepository quoteRepository;

  @Autowired
  private InstrumentsRepository instrumentRepository;

  private Instrument instrument;

  @BeforeEach
  void setup() {
    // Create an Instrument and save it to the database
    instrument = Instrument.builder().isin("TEST_ISIN_123")
      .description("Test Instrument")
      .build();
    instrumentRepository.save(instrument);

    // Add some test quotes for the instrument
    Quote quote1 = Quote.builder()
      .instrument(instrument)
      .price(100.0)
      .createdTime(Instant.now().minusSeconds(1800)).build(); // 30 minutes ago
    System.out.println("quote1 ::  " +quote1.getCreatedTime().toString());
    quoteRepository.save(quote1);

    Quote quote2 = Quote.builder()
      .instrument(instrument)
      .price(110.0)
      .createdTime(Instant.now().minusSeconds(1500)).build();// 25 minutes ago
    System.out.println("quote2 ::  " +quote2.getCreatedTime().toString());
    quoteRepository.save(quote2);

    Quote quote3 = Quote.builder()
      .instrument(instrument)
      .price(105.0)
      .createdTime(Instant.now().minusSeconds(600)).build();// 10 minutes ago
    System.out.println("quote3 ::  " +quote3.getCreatedTime().toString());
    quoteRepository.save(quote3);
  }

  @Test
  void testFindQuotesForInstrumentInTimePeriod() {
    // Set the time range (last 30 minutes)
    Instant endTime = Instant.now();
    Instant startTime = endTime.minusSeconds(30 * 60);
    System.out.println("startTime ::  " +startTime.toString());
    System.out.println("endTime ::  " +endTime.toString());

    // Call the repository method
    List<Quote> quotes = quoteRepository.findQuotesForInstrumentInTimePeriod(
      instrument.getIsin(), startTime, endTime);

    // Assert the number of returned quotes
    assertThat(quotes).hasSize(2);

    // Assert the order and values of quotes
    //assertThat(quotes.get(0).getPrice()).isEqualTo(100.0);
    assertThat(quotes.get(0).getPrice()).isEqualTo(110.0);
    assertThat(quotes.get(1).getPrice()).isEqualTo(105.0);
  }

  @Test
  void testFindQuotesForInstrumentInShorterTimePeriod() {
    // Set a shorter time range (last 15 minutes)
    Instant endTime = Instant.now();
    Instant startTime = endTime.minusSeconds(15 * 60);

    // Call the repository method
    List<Quote> quotes = quoteRepository.findQuotesForInstrumentInTimePeriod(
      instrument.getIsin(), startTime, endTime);

    // Assert the number of returned quotes
    assertThat(quotes).hasSize(1);

    // Assert the quote values
    assertThat(quotes.get(0).getPrice()).isEqualTo(105.0);
  }
}
