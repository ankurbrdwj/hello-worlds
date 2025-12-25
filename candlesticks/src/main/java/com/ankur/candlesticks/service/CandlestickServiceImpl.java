package com.ankur.candlesticks.service;

import com.ankur.candlesticks.dto.Candlestick;
import com.ankur.candlesticks.entity.Quote;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CandlestickServiceImpl implements CandlestickService {

    private final QuoteService quoteService;

    public CandlestickServiceImpl(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * Build candlesticks from a list of Quote entities.
     * Groups quotes by minute and creates OHLC candlesticks.
     *
     * @param quotes List of Quote entities (from database)
     * @return List of Candlesticks grouped by minute, sorted by time
     */
    @Override
    public List<Candlestick> buildCandlesticksFromQuotes(List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort quotes by createdTime
        List<Quote> sortedQuotes = new ArrayList<>(quotes);
        sortedQuotes.sort(Comparator.comparing(Quote::getCreatedTime));

        // Group quotes by minute (truncated to minute)
        Map<LocalDateTime, Candlestick> candlesByMinute = new LinkedHashMap<>();

        for (Quote quote : sortedQuotes) {
            Instant createdTime = quote.getCreatedTime();
            LocalDateTime quoteTime = LocalDateTime.ofInstant(createdTime, ZoneId.systemDefault());
            LocalDateTime minuteKey = quoteTime.truncatedTo(ChronoUnit.MINUTES);
            double price = quote.getPrice();

            Candlestick candle = candlesByMinute.get(minuteKey);

            if (candle == null) {
                // First quote in this minute - create new candlestick
                candle = new Candlestick();
                candle.setOpenTimestamp(minuteKey);
                candle.setOpenPrice(price);
                candle.setHighPrice(price);
                candle.setLowPrice(price);
                candle.setClosePrice(price);
                candle.setCloseTimestamp(quoteTime);
                candlesByMinute.put(minuteKey, candle);
            } else {
                // Update existing candlestick
                candle.setHighPrice(Math.max(candle.getHighPrice(), price));
                candle.setLowPrice(Math.min(candle.getLowPrice(), price));
                candle.setClosePrice(price);
                candle.setCloseTimestamp(quoteTime);
            }
        }

        return new ArrayList<>(candlesByMinute.values());
    }

  @Override
  public List<Candlestick> getCandlesticks(String isin, int minutes) {
    List<Quote> quoteList = quoteService.getByIsinAndMinutes(isin, minutes);
    return buildCandlesticksFromQuotes(quoteList);
  }
}
