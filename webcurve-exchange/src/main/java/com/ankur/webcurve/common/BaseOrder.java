package com.ankur.webcurve.common;

import java.io.Serializable;
/**
 * Industry term: Order — Core Data Model
 *
 * An order is a formal instruction from a market participant to buy or sell a
 * financial instrument at a given price and quantity. BaseOrder holds the fields
 * that are common to both exchange-side orders and client-side orders:
 *
 * - code:     instrument identifier (ISIN, ticker, e.g. "0005.HK") — FIX tag 55 (Symbol)
 * - side:     BID (buy) or ASK (sell) — FIX tag 54 (Side), '1'=Buy / '2'=Sell
 * - quantity: number of shares/contracts — FIX tag 38 (OrderQty)
 * - price:    limit price — FIX tag 44 (Price)
 * - broker:   submitting firm identifier — FIX tag 49 (SenderCompID)
 * - orderID:  exchange-assigned unique identifier — FIX tag 37 (OrderID)
 *
 * In production OMS/EMS systems (Bloomberg AIM, Fidessa), these same fields map
 * directly to FIX protocol tags sent over the wire to the exchange.
 */
public class BaseOrder implements Serializable, Cloneable {
	public static enum SIDE { BID, ASK }
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8463848083656731711L;
	
	// get/set
	public long getOrderID() {
		return orderID;
	}

	public String getCode() {
		return code;
	}

	public SIDE getSide() {
		return side;
	}

	public int getQuantity() {
		return quantity;
	}

	public double getPrice() {
		return price;
	}

	public String getBroker() {
		return broker;
	}

	public void setOrderID(long orderID) {
		this.orderID = orderID;
	}

	protected void setCode(String code) {
		this.code = code;
	}

	protected void setSide(SIDE side) {
		this.side = side;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	private long orderID;
	private String code;
	private SIDE side;
	private int quantity;
	private double price;
	private String broker;

    public BaseOrder ( String code, SIDE side, int quantity, double price, String broker)
    {
        this.code = code;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.broker = broker;
    }
	public static String sideToString(SIDE side)
	{
		if (side == SIDE.BID)
			return "B";
		if (side == SIDE.ASK)
			return "A";
		return "";
		
	}
}
