package com.ankur.webcurve.common;
/**
 * Industry term: Event Callback / Observer Interface
 *
 * Defines the contract for any component that wants to receive exchange events.
 * This is the Observer pattern — the exchange (subject) notifies all registered
 * listeners (observers) whenever something changes, without knowing who they are.
 *
 * In the industry, equivalent interfaces appear everywhere:
 * - FIX Application interface: onMessage(), fromApp(), toApp() callbacks
 * - Market data APIs: Bloomberg's SubscriptionEventHandler, Reuters' EventListener
 * - Algo framework callbacks: onOrderFill(), onBookUpdate(), onTrade()
 *
 * Any class implementing this interface — whether it's the Swing GUI, the FIX
 * gateway, or an audit logger — plugs into the exchange event stream without
 * any change to the exchange itself. This is the Open/Closed Principle in action.
 *
 * @param <T> the event type: Order, Trade, or OrderBook
 */
public interface ExchangeEventListener<T> {
	public void onChangeEvent(T t);
}
