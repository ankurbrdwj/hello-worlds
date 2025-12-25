package com.ankur.candlesticks.controller;

import com.ankur.candlesticks.dto.Candlestick;
import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.entity.Quote;

import com.ankur.candlesticks.service.CandlestickService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ankur.candlesticks.service.InstrumentService;
import com.ankur.candlesticks.service.QuoteService;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
public class CandlestickController {

  private final InstrumentService instrumentService;
  private final QuoteService quoteService;
  private final CandlestickService candlestickService;

  // Fetch instruments from the last 'n' minutes provided by the user
  @GetMapping("/instruments")
  public List<Instrument> getInstruments(@RequestParam(defaultValue = "30") int minutes) {
    return instrumentService.getInstrumentsFromLastMinutes(minutes);
  }

  // Fetch quotes from the last 'n' minutes provided by the user
  @GetMapping("/quotes")
  public List<Quote> getQuotes(@RequestParam(defaultValue = "30") int minutes) {
    return quoteService.getQuotesFromLastMinutes(minutes);
  }
  @GetMapping("/candlesticks")
  public List<Candlestick> getQuotesForIsin(@RequestParam String isin, @RequestParam(defaultValue = "30") int minutes) {


    // Fetch quotes from the service for the given time period
    return candlestickService.getCandlesticks(isin, minutes);
  }
}
