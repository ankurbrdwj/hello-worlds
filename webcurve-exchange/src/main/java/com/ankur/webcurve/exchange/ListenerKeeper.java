package com.ankur.webcurve.exchange;

import com.ankur.webcurve.common.ExchangeEventListener;

import java.util.Vector;

/**
 * Industry term: Event Bus / Publish-Subscribe Registry
 *
 * Trading systems are fundamentally event-driven. Every order state change, trade,
 * or price update is an event that multiple downstream consumers need simultaneously —
 * the GUI, risk engine, FIX gateway, audit logger, and algo strategies all need the
 * same stream of events without being coupled to each other.
 *
 * ListenerKeeper is the in-process event bus implementing the Observer pattern.
 * In production, this role is played by messaging middleware:
 * - Kafka: durable, high-throughput event streaming (order/trade events)
 * - Solace / TIBCO EMS: ultra-low-latency pub/sub for market data distribution
 * - Aeron: sub-microsecond IPC for HFT co-located components
 *
 * The Exchange owns three instances: orderListenerKeeper, tradeListenerKeeper,
 * and orderBookListenerKeeper — one event bus per domain event type.
 *
 * @param <T> the event type this keeper distributes (Order, Trade, or OrderBook)
 */
public class ListenerKeeper<T> {
    private Vector<ExchangeEventListener<T>> exchangeListeners = new Vector<ExchangeEventListener<T>>();

    public void addExchangeListener(ExchangeEventListener<T> listener) {
        if (null == listener)
            return;
        if (!exchangeListeners.contains(listener))
            exchangeListeners.add(listener);
    }

    public void removeExchangeListener(ExchangeEventListener<T> listener) {
        exchangeListeners.remove(listener);
    }

    public void updateExchangeListeners(T t) {
        for (ExchangeEventListener<T> item : exchangeListeners) {
            try {
                item.onChangeEvent(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
