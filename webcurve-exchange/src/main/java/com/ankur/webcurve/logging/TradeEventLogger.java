package com.ankur.webcurve.logging;

import com.ankur.webcurve.common.ExchangeEventListener;
import com.ankur.webcurve.common.Trade;
import lombok.extern.slf4j.Slf4j;

/**
 * Industry term: Trade Report / Post-Trade Audit Trail
 *
 * Every executed trade must be recorded and reported. In the industry, a trade
 * event triggers multiple downstream obligations simultaneously:
 *
 * 1. Post-trade reporting: under MiFID II (Europe), Dodd-Frank (US), and local
 *    rules, every trade must be published to an APA (Approved Publication
 *    Arrangement) or Trade Repository within seconds of execution — including
 *    price, quantity, timestamp, instrument, and counterparty identifiers.
 *
 * 2. Clearing and settlement: the trade is sent to a central counterparty (CCP)
 *    such as LCH, DTCC, or HKCC, which novates the trade (becoming buyer to
 *    every seller and seller to every buyer) to eliminate counterparty credit risk.
 *
 * 3. P&L and position updates: risk systems consume the trade stream in real time
 *    to update trader positions, compute intraday P&L, and enforce risk limits.
 *
 * This class logs the buyer broker, seller broker, price, and quantity — the
 * minimum fields required to reconstruct a complete trade history for any instrument.
 * Follows SRP: only this class changes if the trade log format changes.
 */
@Slf4j
public class TradeEventLogger implements ExchangeEventListener<Trade> {

    @Override
    public void onChangeEvent(Trade trade) {
        log.info("[TRADE] id={} code={} qty={} price={} buyer={} seller={}",
                trade.getTradeID(), trade.getAskOrder().getCode(),
                trade.getQuantity(), trade.getPrice(),
                trade.getBidOrder().getBroker(), trade.getAskOrder().getBroker());
    }
}