package com.ankur.webcurve.client;



import com.ankur.webcurve.common.BaseOrder;
import com.ankur.webcurve.common.Order;
import com.ankur.webcurve.common.PriceStepTable;
import com.ankur.webcurve.exchange.Exchange;
import com.ankur.webcurve.exchange.OrderBook;

import java.util.Random;

/**
 * Industry term: Market Maker / Liquidity Provider
 *
 * A market maker is a firm or individual that continuously quotes both a buy
 * and sell price for a financial instrument, standing ready to trade at those
 * prices. By always being present on both sides, market makers provide liquidity —
 * without them, other participants could not easily find a counterparty to trade with.
 *
 * In real markets, designated market makers (DMMs) on NYSE, or electronic
 * liquidity providers like Citadel Securities, Virtu Financial, and Jane Street,
 * post millions of quotes per second. Their revenue comes from the bid-ask spread
 * (buying at the bid, selling at the ask) and exchange rebates.
 *
 * This class simulates that behaviour: it runs as a background thread, continuously
 * generating random LIMIT orders on both sides of the book at a price derived from
 * a base price plus a random walk (priceVariant in basis points). The priceStep
 * enforcement ensures all prices comply with the HKEX tick size table — in real
 * markets, submitting an off-tick price results in immediate order rejection.
 *
 * The random interval between orders (tradingMinInterval to tradingMaxInterval)
 * models the stochastic quoting behaviour of a real algorithmic market maker.
 */
public class MarketParticipant extends Thread {
	protected Exchange exchange;
	protected String stock;
	protected Double basePrice;
	protected Integer priceVariant;

	protected Double sd;
	protected Double sdFactor;
	protected Integer tradingLength;
	protected Integer tradingMinInterval;
	protected Integer tradingMaxInterval;
	protected Integer minQuantity;
	protected Integer maxQuantity;
	protected PriceStepTable priceStep = new PriceStepTable();
	
	private boolean started = false;
	private boolean paused = false;

	public MarketParticipant( Exchange exchange, String stock, Double basePrice,
			Integer priceVariant, // price variant in base points
			Double sd, Double sdFactor, //price change in standard deviation (not yet implemented)
			Integer minQuantity, Integer maxQuantity, 
			Integer tradingLength, // time length of trading in minutes (not yet implemented)
			Integer tradingMinInterval,// min interval in milliseconds
			Integer tradingMaxInterval // max interval in milliseconds
			)
	{
		super("MarketParticipant");
		this.exchange = exchange;
		this.stock = stock;
		this.basePrice = basePrice;
		this.priceVariant = priceVariant;
		this.sd = sd;
		this.sdFactor = sdFactor;
		this.minQuantity = minQuantity;
		this.maxQuantity = maxQuantity;
		this.tradingLength = tradingLength;
		this.tradingMinInterval = tradingMinInterval;
		this.tradingMaxInterval = tradingMaxInterval;
	}

	public void run()
	{
		OrderBook book = exchange.getBook(stock);
		while (true)
		{		
			try {
	            synchronized(this) {
					while (paused)
							wait();
	            }
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// generating random order
			Random ran = new Random();
			BaseOrder.SIDE side = BaseOrder.SIDE.ASK;
			if (ran.nextBoolean())
				side = BaseOrder.SIDE.BID;
			Double sign = ran.nextBoolean()?1.0:-1.0;
			Double price = basePrice *(1 + sign * ran.nextInt(priceVariant) / 10000.0);
			price = priceStep.getRoundedPrice(exchange.getName(), stock, price);

			if (side == BaseOrder.SIDE.BID)
			{
				Double bestAsk = book.getBestAsk();
				price = (null != bestAsk && price > bestAsk)?bestAsk:price;
			}
			else
			{
				Double bestBid = book.getBestBid();
				price = (null != bestBid && price < bestBid)?bestBid:price;
			}
				
			int quantity = (ran.nextInt(maxQuantity-minQuantity) + minQuantity)/ minQuantity * minQuantity;
			
			exchange.enterOrder(stock, Order.TYPE.LIMIT, side, quantity, price, "MarketMaker", "");
		
			try {
				sleep(tradingMinInterval + ran.nextInt(tradingMaxInterval-tradingMinInterval));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	synchronized public void start()
	{
		if (!started)
		{
			started = true;
			super.start();
		}
		
		if (paused)
		{
			notify();
			paused = false;
			return;		
		}
	}
	
	synchronized public void pause()
	{
		paused = true;
	}
	

	// getters and setters
	public String getStock() {
		return stock;
	}

	public void setStock(String stock) {
		this.stock = stock;
	}

	public Double getBasePrice() {
		return basePrice;
	}

	public void setBasePrice(Double basePrice) {
		this.basePrice = basePrice;
	}

	public Integer getPriceVariant() {
		return priceVariant;
	}

	public void setPriceVariant(Integer priceVariant) {
		this.priceVariant = priceVariant;
	}

	public Double getSd() {
		return sd;
	}

	public void setSd(Double sd) {
		this.sd = sd;
	}

	public Double getSdFactor() {
		return sdFactor;
	}

	public void setSdFactor(Double sdFactor) {
		this.sdFactor = sdFactor;
	}

	public Integer getTradingLength() {
		return tradingLength;
	}

	public void setTradingLength(Integer tradingLength) {
		this.tradingLength = tradingLength;
	}

	public Integer getTradingMinInterval() {
		return tradingMinInterval;
	}

	public void setTradingMinInterval(Integer tradingMinInterval) {
		this.tradingMinInterval = tradingMinInterval;
	}

	public Integer getTradingMaxInterval() {
		return tradingMaxInterval;
	}

	public void setTradingMaxInterval(Integer tradingMaxInterval) {
		this.tradingMaxInterval = tradingMaxInterval;
	}

	public Integer getMinQuantity() {
		return minQuantity;
	}

	public void setMinQuantity(Integer minQuantity) {
		this.minQuantity = minQuantity;
	}

	public Integer getMaxQuantity() {
		return maxQuantity;
	}

	public void setMaxQuantity(Integer maxQuantity) {
		this.maxQuantity = maxQuantity;
	}

	public PriceStepTable getPriceStep() {
		return priceStep;
	}

	public void setPriceStep(PriceStepTable priceStep) {
		this.priceStep = priceStep;
	}
	
}
