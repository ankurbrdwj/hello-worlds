package com.ankur.candlesticks.service;

import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.entity.Quote;
import com.ankur.candlesticks.repository.InstrumentsRepository;
import com.ankur.candlesticks.repository.QuoteRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {
  private final Map<String, String> quotes = new ConcurrentHashMap<>();

  private final QuoteRepository quoteRepository;
  private final InstrumentsRepository instrumentRepository;

  public List<Quote> getQuotesFromLastMinutes(int minutes) {
    Instant now = Instant.now();
    Instant minutesAgo = now.minus(minutes, ChronoUnit.MINUTES);
    return quoteRepository.findQuotesAfter(minutesAgo);
  }

  @Override
  public List<Quote> getByIsinAndMinutes(String isin, int minutes) {
    // Get current time and subtract 30 minutes (or the value provided in minutes)
    Instant endTime = Instant.now();
    Instant startTime = endTime.minus(minutes, ChronoUnit.MINUTES);
    return quoteRepository.findQuotesForInstrumentInTimePeriod(isin,startTime,endTime);
  }

  @Override
  public void processQuote(String isin, double price) {

  }

  @Override
  public void saveQuote(String isin, double price) {
    // Try to find the Instrument by ISIN
    Instrument instrument = instrumentRepository.findByIsin(isin);

    // Check if the instrument is null
    if (instrument == null) {
      log.error("Instrument with ISIN {} not found.", isin);
      //throw new IllegalArgumentException("Instrument with ISIN " + isin + " not found");
    }else {

      // Create a new Quote object if the instrument exists
      Quote quote = Quote.builder()
        .setInstrument(instrument) // Set the instrument
        .setPrice(price)           // Set the price
        .build();

      // Save the quote to the repository
      quoteRepository.save(quote);

      // Optionally, update the quotes map
      quotes.put(isin, String.valueOf(price));
    }
  }
}
