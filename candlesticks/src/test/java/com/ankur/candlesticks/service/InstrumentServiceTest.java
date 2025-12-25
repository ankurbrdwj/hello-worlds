package com.ankur.candlesticks.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.repository.InstrumentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class InstrumentServiceTest {

  private InstrumentService instrumentService;

  @Mock
  InstrumentsRepository instrumentsRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    instrumentService = new InstrumentServiceImpl(instrumentsRepository);
  }

  @Test
  public void testAddInstrument() {
    // Given: no existing instrument
    when(instrumentsRepository.findByIsin("ABC123")).thenReturn(null);

    // When
    instrumentService.addInstrument("ABC123", "Test Instrument");

    // Then: verify save was called
    verify(instrumentsRepository).save(any(Instrument.class));
  }

  @Test
  public void testAddInstrument_Reactivate() {
    // Given: existing inactive instrument (ISIN reuse)
    Instrument existing = Instrument.builder()
        .setIsin("ABC123")
        .setDescription("Old Description")
        .setActive(false)
        .build();
    when(instrumentsRepository.findByIsin("ABC123")).thenReturn(existing);

    // When
    instrumentService.addInstrument("ABC123", "New Description");

    // Then: should reactivate
    assertTrue(existing.isActive());
    verify(instrumentsRepository).save(existing);
  }

  @Test
  public void testDeleteInstrument() {
    // Given: existing active instrument
    Instrument existing = Instrument.builder()
        .setIsin("ABC123")
        .setDescription("Test Instrument")
        .setActive(true)
        .build();
    when(instrumentsRepository.findByIsin("ABC123")).thenReturn(existing);

    // When
    instrumentService.deleteInstrument("ABC123");

    // Then: should deactivate (soft delete)
    assertFalse(existing.isActive());
    verify(instrumentsRepository).save(existing);
  }

  @Test
  public void testInstrumentExists() {
    when(instrumentsRepository.existsByIsinAndActive("ABC123", true)).thenReturn(true);
    when(instrumentsRepository.existsByIsinAndActive("XYZ999", true)).thenReturn(false);

    assertTrue(instrumentService.instrumentExists("ABC123"));
    assertFalse(instrumentService.instrumentExists("XYZ999"));
  }
}
