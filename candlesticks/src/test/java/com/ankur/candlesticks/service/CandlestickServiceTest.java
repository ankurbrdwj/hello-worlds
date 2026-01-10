package com.ankur.candlesticks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.ankur.candlesticks.dto.Candlestick;
import com.ankur.candlesticks.entity.Instrument;
import com.ankur.candlesticks.entity.Quote;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CandlestickServiceTest {

    private static final String ISIN = "MB51585238Q5";
    private static final String DESCRIPTION = "vero fames adipisci";

    // Real quote prices from partner service
    private static final double QUOTE_1 = 1089.6053;  // First (open)
    private static final double QUOTE_2 = 1053.9474;  // Lowest
    private static final double QUOTE_3 = 1094.2895;  // Highest
    private static final double QUOTE_4 = 1079.6316;  // Last (close)

    @Mock
    QuoteService quoteService;

    CandlestickServiceImpl candlestickService;

    @BeforeEach
    void setUp() {
        candlestickService = new CandlestickServiceImpl(quoteService);
    }

    private Quote createQuote(String isin, double price, Instant createdTime) {
        Instrument instrument = Instrument.builder()
                .setIsin(isin)
                .setDescription(DESCRIPTION)
                .setActive(true)
                .build();

        Quote quote = Quote.builder()
                .setInstrument(instrument)
                .setPrice(price)
                .build();
        quote.setCreatedTime(createdTime);
        return quote;
    }

    // ==================== buildCandlesticksFromQuotes TESTS ====================

    @Test
    public void testBuildCandlesticksFromQuotes_SingleMinute() {
        Instant baseTime = Instant.parse("2024-12-24T13:00:00Z");
        List<Quote> quotes = new ArrayList<>();
        quotes.add(createQuote(ISIN, QUOTE_1, baseTime.plusSeconds(5)));
        quotes.add(createQuote(ISIN, QUOTE_2, baseTime.plusSeconds(15)));
        quotes.add(createQuote(ISIN, QUOTE_3, baseTime.plusSeconds(30)));
        quotes.add(createQuote(ISIN, QUOTE_4, baseTime.plusSeconds(45)));

        List<Candlestick> candles = candlestickService.buildCandlesticksFromQuotes(quotes);

        assertEquals(1, candles.size());
        Candlestick candle = candles.getFirst();
        assertEquals(QUOTE_1, candle.getOpenPrice(), "Open should be first quote");
        assertEquals(QUOTE_3, candle.getHighPrice(), "High should be max (1094.2895)");
        assertEquals(QUOTE_2, candle.getLowPrice(), "Low should be min (1053.9474)");
        assertEquals(QUOTE_4, candle.getClosePrice(), "Close should be last quote");
    }

    @Test
    public void testBuildCandlesticksFromQuotes_MultipleMinutes() {
        Instant min0 = Instant.parse("2024-12-24T13:00:15Z");
        Instant min1a = Instant.parse("2024-12-24T13:01:20Z");
        Instant min1b = Instant.parse("2024-12-24T13:01:45Z");
        Instant min2a = Instant.parse("2024-12-24T13:02:10Z");
        Instant min2b = Instant.parse("2024-12-24T13:02:55Z");

        List<Quote> quotes = new ArrayList<>();
        quotes.add(createQuote(ISIN, QUOTE_1, min0));
        quotes.add(createQuote(ISIN, QUOTE_2, min1a));
        quotes.add(createQuote(ISIN, QUOTE_3, min1b));
        quotes.add(createQuote(ISIN, QUOTE_4, min2a));
        quotes.add(createQuote(ISIN, 1100.0, min2b));

        List<Candlestick> candles = candlestickService.buildCandlesticksFromQuotes(quotes);

        assertEquals(3, candles.size(), "Should have 3 candlesticks for 3 minutes");

        // Candle 0 (13:00) - 1 quote
        Candlestick c0 = candles.get(0);
        assertEquals(QUOTE_1, c0.getOpenPrice());
        assertEquals(QUOTE_1, c0.getHighPrice());
        assertEquals(QUOTE_1, c0.getLowPrice());
        assertEquals(QUOTE_1, c0.getClosePrice());

        // Candle 1 (13:01) - 2 quotes
        Candlestick c1 = candles.get(1);
        assertEquals(QUOTE_2, c1.getOpenPrice());
        assertEquals(QUOTE_3, c1.getHighPrice());
        assertEquals(QUOTE_2, c1.getLowPrice());
        assertEquals(QUOTE_3, c1.getClosePrice());

        // Candle 2 (13:02) - 2 quotes
        Candlestick c2 = candles.get(2);
        assertEquals(QUOTE_4, c2.getOpenPrice());
        assertEquals(1100.0, c2.getHighPrice());
        assertEquals(QUOTE_4, c2.getLowPrice());
        assertEquals(1100.0, c2.getClosePrice());
    }

    @Test
    public void testBuildCandlesticksFromQuotes_EmptyList() {
        List<Quote> quotes = new ArrayList<>();

        List<Candlestick> candles = candlestickService.buildCandlesticksFromQuotes(quotes);

        assertTrue(candles.isEmpty(), "Empty quotes should return empty candlesticks");
    }

    @Test
    public void testBuildCandlesticksFromQuotes_UnsortedQuotes() {
        Instant time1 = Instant.parse("2024-12-24T13:00:05Z");
        Instant time2 = Instant.parse("2024-12-24T13:00:30Z");
        Instant time3 = Instant.parse("2024-12-24T13:00:15Z");
        Instant time4 = Instant.parse("2024-12-24T13:00:45Z");

        // Quotes added out of order
        List<Quote> quotes = new ArrayList<>();
        quotes.add(createQuote(ISIN, QUOTE_2, time2));
        quotes.add(createQuote(ISIN, QUOTE_1, time1));
        quotes.add(createQuote(ISIN, QUOTE_4, time4));
        quotes.add(createQuote(ISIN, QUOTE_3, time3));

        List<Candlestick> candles = candlestickService.buildCandlesticksFromQuotes(quotes);

        assertEquals(1, candles.size());
        Candlestick candle = candles.getFirst();

        assertEquals(QUOTE_1, candle.getOpenPrice(), "Open should be earliest quote");
        assertEquals(QUOTE_4, candle.getClosePrice(), "Close should be latest quote");
        assertEquals(QUOTE_3, candle.getHighPrice(), "High should be max");
        assertEquals(QUOTE_2, candle.getLowPrice(), "Low should be min");
    }

    @Test
    public void testBuildCandlesticksFromQuotes_RealPartnerData() {
        Instant baseTime = Instant.parse("2024-12-24T13:00:00Z");

        List<Quote> quotes = new ArrayList<>();
        quotes.add(createQuote(ISIN, 1089.6053, baseTime.plusSeconds(5)));
        quotes.add(createQuote(ISIN, 1053.9474, baseTime.plusSeconds(15)));
        quotes.add(createQuote(ISIN, 1094.2895, baseTime.plusSeconds(30)));
        quotes.add(createQuote(ISIN, 1079.6316, baseTime.plusSeconds(45)));

        List<Candlestick> candles = candlestickService.buildCandlesticksFromQuotes(quotes);

        assertEquals(1, candles.size());
        Candlestick candle = candles.getFirst();

        assertEquals(1089.6053, candle.getOpenPrice(), "Open = first quote");
        assertEquals(1094.2895, candle.getHighPrice(), "High = max");
        assertEquals(1053.9474, candle.getLowPrice(), "Low = min");
        assertEquals(1079.6316, candle.getClosePrice(), "Close = last quote");
    }

    // ==================== getCandlesticks() TESTS (with mocks) ====================

    @Test
    public void testGetCandlesticks_ReturnsCorrectOHLC() {
        Instant baseTime = Instant.parse("2024-12-24T13:00:00Z");
        List<Quote> mockQuotes = new ArrayList<>();
        mockQuotes.add(createQuote(ISIN, QUOTE_1, baseTime.plusSeconds(5)));
        mockQuotes.add(createQuote(ISIN, QUOTE_2, baseTime.plusSeconds(15)));
        mockQuotes.add(createQuote(ISIN, QUOTE_3, baseTime.plusSeconds(30)));
        mockQuotes.add(createQuote(ISIN, QUOTE_4, baseTime.plusSeconds(45)));

        when(quoteService.getByIsinAndMinutes(any(), anyInt())).thenReturn(mockQuotes);

        List<Candlestick> candles = candlestickService.getCandlesticks(ISIN, 30);

        assertEquals(1, candles.size());
        Candlestick candle = candles.getFirst();
        assertEquals(QUOTE_1, candle.getOpenPrice());
        assertEquals(QUOTE_3, candle.getHighPrice());
        assertEquals(QUOTE_2, candle.getLowPrice());
        assertEquals(QUOTE_4, candle.getClosePrice());
    }

    @Test
    public void testGetCandlesticks_NoQuotes_ReturnsEmptyList() {
        when(quoteService.getByIsinAndMinutes(any(), anyInt())).thenReturn(new ArrayList<>());

        List<Candlestick> candles = candlestickService.getCandlesticks(ISIN, 30);

        assertTrue(candles.isEmpty());
    }

    @Test
    public void testGetCandlesticks_MultipleMinutes() {
        Instant min0 = Instant.parse("2024-12-24T13:00:15Z");
        Instant min1 = Instant.parse("2024-12-24T13:01:20Z");
        Instant min2 = Instant.parse("2024-12-24T13:02:10Z");

        List<Quote> mockQuotes = new ArrayList<>();
        mockQuotes.add(createQuote(ISIN, 100.0, min0));
        mockQuotes.add(createQuote(ISIN, 200.0, min1));
        mockQuotes.add(createQuote(ISIN, 300.0, min2));

        when(quoteService.getByIsinAndMinutes(any(), anyInt())).thenReturn(mockQuotes);

        List<Candlestick> candles = candlestickService.getCandlesticks(ISIN, 30);

        assertEquals(3, candles.size());
        assertEquals(100.0, candles.get(0).getOpenPrice());
        assertEquals(200.0, candles.get(1).getOpenPrice());
        assertEquals(300.0, candles.get(2).getOpenPrice());
    }
}