package com.ankur.webcurve.logging;

import com.ankur.webcurve.common.ExchangeEventListener;
import com.ankur.webcurve.common.Order;
import lombok.extern.slf4j.Slf4j;

/**
 * Industry term: Order Audit Trail / Trade Surveillance — Order Stream
 *
 * Regulators globally (SEC, FCA, ESMA, SFC, ASIC) require exchanges and brokers
 * to maintain a complete, immutable, timestamped record of every order event:
 * every new order, every fill, every amendment, every cancellation.
 *
 * This audit trail serves three purposes in the industry:
 * 1. Trade surveillance: detecting market manipulation (spoofing, layering,
 *    front-running) by replaying the order sequence around suspicious trades
 * 2. Regulatory reporting: MiFID II RTS 24 requires firms to keep order records
 *    for 5 years; Dodd-Frank requires similar retention in the US
 * 3. Incident investigation: reconstructing what happened during a flash crash
 *    or erroneous trade event
 *
 * In production, order events are written to append-only, immutable storage —
 * Kafka topics (retained indefinitely), WORM (Write Once Read Many) disk arrays,
 * or specialist surveillance platforms like NICE Actimize or Nasdaq Surveillance.
 *
 * This class follows SRP: it has exactly one responsibility — log order events.
 * If the order log format changes, only this class changes.
 */
@Slf4j
public class OrderEventLogger implements ExchangeEventListener<Order> {

    @Override
    public void onChangeEvent(Order order) {
        log.info("[ORDER] id={} code={} side={} type={} status={} qty={} price={} broker={}",
                order.getOrderID(), order.getCode(), order.getSide(), order.getType(),
                order.getStatus(), order.getQuantity(), order.getPrice(), order.getBroker());
    }
}