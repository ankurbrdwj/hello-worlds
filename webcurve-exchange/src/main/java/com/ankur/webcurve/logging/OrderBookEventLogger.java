package com.ankur.webcurve.logging;

import com.ankur.webcurve.common.ExchangeEventListener;
import com.ankur.webcurve.exchange.OrderBook;
import lombok.extern.slf4j.Slf4j;

/**
 * Industry term: Market Data Audit Trail / Level 2 Snapshot Log
 *
 * "Level 2" market data — the full order book showing multiple bid and ask price
 * levels with their quantities — is a core product that exchanges sell to data
 * vendors (Bloomberg, Refinitiv) and directly to professional traders.
 *
 * Logging order book snapshots serves several purposes in industry:
 * 1. Market reconstruction: regulators can replay the exact state of the book
 *    at any point in time around a suspicious event or erroneous trade
 * 2. Backtesting: strategies that use order book depth (e.g. iceberg detection,
 *    queue position modelling) require historical LOB snapshots to backtest
 * 3. Best execution analysis: MiFID II requires firms to demonstrate they achieved
 *    "best execution" — comparing fills against the prevailing order book state
 *
 * The VWAP logged here is the running session VWAP — a standard benchmark.
 * Institutional traders are evaluated against VWAP: executing better than VWAP
 * is considered good execution; worse than VWAP requires explanation.
 *
 * Follows SRP: only this class changes if the book snapshot log format changes.
 */
@Slf4j
public class OrderBookEventLogger implements ExchangeEventListener<OrderBook> {

    @Override
    public void onChangeEvent(OrderBook book) {
        log.info("[BOOK]  code={} bestBid={} bestAsk={} bids={} asks={} trades={} VWAP={}",
                book.getCode(), book.getBestBid(), book.getBestAsk(),
                book.getBidOrders().size(), book.getAskOrders().size(),
                book.getTrades().size(), String.format("%.4f", book.getVWAP()));
    }
}