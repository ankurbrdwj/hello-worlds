package com.ankur.exchange.feed.service;

import com.ankur.exchange.feed.generator.RandomWalkInstrument;
import com.ankur.exchange.feed.model.Instrument;
import com.ankur.exchange.feed.model.Quote;
import com.ankur.exchange.feed.model.StockData;
import com.ankur.exchange.feed.util.StockList;
import com.ankur.exchange.feed.websocket.InstrumentWebSocketHandler;
import com.ankur.exchange.feed.websocket.QuotesWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataGenerator {

    private final InstrumentWebSocketHandler instrumentHandler;
    private final QuotesWebSocketHandler quotesHandler;

    @Value("${market.feed.min.instruments:5}")
    private int minNumberOfInstruments;

    @Value("${market.feed.max.instruments:10}")
    private int maxNumberOfInstruments;

    @Value("${market.feed.avg.millis.between.quotes:100}")
    private int avgMillisecondsBetweenQuotes;

    private final List<RandomWalkInstrument> instruments = new CopyOnWriteArrayList<>();
    private final List<StockData> availableStocks = new CopyOnWriteArrayList<>();
    private final Set<String> activeSymbols = Collections.synchronizedSet(new HashSet<>());
    private final Random random = new Random();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // Initialize available stocks
        availableStocks.addAll(StockList.STOCK_LIST);

        // Start the generator asynchronously
        startGenerator();
    }

    @Async
    public void startGenerator() {
        log.info("Starting Market Data Generator");

        // Add initial instruments
        for (int i = 0; i < minNumberOfInstruments; i++) {
            addInstrument();
        }

        // Main generation loop
        while (true) {
            try {
                // Maybe add a new instrument
                if (random.nextDouble() < 0.1
                        && instruments.size() < maxNumberOfInstruments
                        && !availableStocks.isEmpty()) {
                    addInstrument();
                }

                // Maybe remove an instrument
                if (random.nextDouble() < 0.05 && instruments.size() > minNumberOfInstruments) {
                    removeInstrument();
                }

                // Generate a price for a random instrument
                if (!instruments.isEmpty()) {
                    RandomWalkInstrument randomInstrument =
                            instruments.get(random.nextInt(instruments.size()));
                    BigDecimal newPrice = randomInstrument.nextPrice();
                    Quote quote = new Quote(randomInstrument.getInstrument().getIsin(), newPrice);
                    quotesHandler.sendPrice(quote);
                }

                Thread.sleep(avgMillisecondsBetweenQuotes);

            } catch (InterruptedException e) {
                log.error("Generator thread interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in generator loop", e);
            }
        }
    }

    private void addInstrument() {
        if (availableStocks.isEmpty()) {
            return;
        }

        // Pick a random stock from available list
        int index = random.nextInt(availableStocks.size());
        StockData stockData = availableStocks.remove(index);
        activeSymbols.add(stockData.getSymbol());

        Instrument instrument = new Instrument(stockData.getSymbol(), stockData.getName());
        RandomWalkInstrument randomWalkInstrument = new RandomWalkInstrument(instrument);
        instruments.add(randomWalkInstrument);
        instrumentHandler.addInstrument(instrument);

        log.info("Added instrument: {} - {}", instrument.getIsin(), instrument.getDescription());
    }

    private void removeInstrument() {
        if (instruments.isEmpty()) {
            return;
        }

        int index = random.nextInt(instruments.size());
        RandomWalkInstrument removed = instruments.remove(index);

        // Return stock to available pool
        String symbol = removed.getInstrument().getIsin();
        activeSymbols.remove(symbol);
        availableStocks.add(new StockData(symbol, removed.getInstrument().getDescription()));

        instrumentHandler.deleteInstrument(removed.getInstrument());

        log.info("Removed instrument: {} - {}",
                removed.getInstrument().getIsin(),
                removed.getInstrument().getDescription());
    }
}
