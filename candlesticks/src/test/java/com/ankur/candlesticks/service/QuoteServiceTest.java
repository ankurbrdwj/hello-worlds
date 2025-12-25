package com.ankur.candlesticks.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.entity.Quote;
import com.ankur.candlesticks.repository.InstrumentsRepository;
import com.ankur.candlesticks.repository.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class QuoteServiceTest {

  private QuoteService quoteService;

  @Mock
  QuoteRepository quoteRepository;

  @Mock
  InstrumentsRepository instrumentRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    quoteService = new QuoteServiceImpl(quoteRepository, instrumentRepository);
  }

  @Test
  public void testSaveQuote_Success() {
    // Given: instrument exists
    Instrument instrument = Instrument.builder()
        .setIsin("ABC123")
        .setDescription("Test Instrument")
        .setActive(true)
        .build();
    when(instrumentRepository.findByIsin("ABC123")).thenReturn(instrument);

    // When
    quoteService.saveQuote("ABC123", 100.0);

    // Then: quote should be saved
    verify(quoteRepository).save(any(Quote.class));
  }

  @Test
  public void testSaveQuote_InstrumentNotFound() {
    // Given: instrument doesn't exist
    when(instrumentRepository.findByIsin("UNKNOWN")).thenReturn(null);

    // When
    quoteService.saveQuote("UNKNOWN", 100.0);

    // Then: quote should NOT be saved
    verify(quoteRepository, never()).save(any(Quote.class));
  }
  @Test
  public void testProcessQuote_IsinDoesNotExist() {
    // Given: instrument doesn't exist
    when(instrumentRepository.findByIsin("NONEXISTENT")).thenReturn(null);

    // When
    quoteService.processQuote("NONEXISTENT", 150.0);

    // Then: quote should NOT be saved
    verify(quoteRepository, never()).save(any(Quote.class));
  }
  @Test
  public void testIsinReuse_ActiveFlagOnly() {
    Instrument instrument = Instrument.builder()
            .setIsin("ABC12")
            .setDescription("Test Instrument")
            .setActive(true)
            .build();

    // Instrument is active: quote should be saved
    when(instrumentRepository.findByIsin("ABC12")).thenReturn(instrument);
    quoteService.saveQuote("ABC12", 100.0);
    verify(quoteRepository).save(any(Quote.class));

    // Instrument is deleted (inactive): quote should NOT be saved
    instrument.setActive(false);
    when(instrumentRepository.findByIsin("ABC12")).thenReturn(instrument);
    quoteService.saveQuote("ABC12", 150.0);
    verify(quoteRepository, never()).save(argThat(q -> q.getPrice() == 150.0));

    // Instrument is re-added (active again): quote should be saved
    instrument.setActive(true);
    when(instrumentRepository.findByIsin("ABC12")).thenReturn(instrument);
    quoteService.saveQuote("ABC12", 200.0);
    verify(quoteRepository).save(argThat(q -> q.getPrice() == 200.0));
  }


}
