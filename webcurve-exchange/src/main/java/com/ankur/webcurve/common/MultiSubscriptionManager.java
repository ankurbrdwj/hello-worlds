package com.ankur.webcurve.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Industry term: Market Data Subscription Manager
 *
 * In institutional trading, clients subscribe to market data by instrument and
 * data type. A fund may want full order book updates for AAPL but only last-trade
 * data for MSFT, and nothing for everything else. The subscription manager maps
 * (instrument, dataType) → [list of listeners], so each consumer only gets
 * exactly the data it requested — avoiding a flood of irrelevant events.
 *
 * This pattern is used extensively in:
 * - Bloomberg B-PIPE: clients subscribe to specific fields per security
 * - Refinitiv Elektron: topic-based pub/sub over RMDS
 * - Exchange direct feeds: NASDAQ TotalView, NYSE OpenBook Ultra
 * - FIX MarketDataRequest (V): a broker subscribes to depth for specific symbols
 *
 * The composite key (type + instrument) prevents collision between, for example,
 * a MarketDepth subscription on "0005.HK" and a MarketTrade subscription on
 * the same symbol — they are treated as distinct subscription streams.
 *
 * @param <K> subscription key type (e.g. String symbol)
 * @param <T> event type (e.g. MarketDepth, MarketTrade)
 */
public class MultiSubscriptionManager<K, T> {

	Map<String, ArrayList<ExchangeEventListener<T>>> subscriptions = Collections.synchronizedMap(new HashMap<String, ArrayList<ExchangeEventListener<T>>>());
	
	String getStringKey(K key, Class<T> type)
	{
		return type.toString() + "-" + key.toString();
	}
	
	public boolean subscribe(K key, Class<T> type, ExchangeEventListener<T> listener)
	{
		ArrayList<ExchangeEventListener<T>> listeners = subscriptions.get(getStringKey(key, type));
		if ( null == listeners)
		{
			listeners = new ArrayList<ExchangeEventListener<T>>();
			subscriptions.put(getStringKey(key, type), listeners);
		}
		else if (listeners.contains(listener))
				return false;
	
		listeners.add(listener);
		return true;
		
	}

	public boolean unsubscribe(K key, Class<T> type, ExchangeEventListener<T> listener)
	{
		ArrayList<ExchangeEventListener<T>> listeners = subscriptions.get(getStringKey(key, type));
		if ( null == listeners)
			return false;
		
		return listeners.remove(listener);
	}
	
	
	public void update(K key, Class<T> type, T t)
	{
		ArrayList<ExchangeEventListener<T>> listeners = subscriptions.get(getStringKey(key, type));
		if ( null == listeners)
			return;
		
		for (ExchangeEventListener<T> listener: listeners)
		{
			listener.onChangeEvent(t);
		}
		
	}
}
