/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ankur.webcurve.common;
import java.io.Serializable;
import java.util.Date;


/**
 * Industry term: Execution / Fill / Trade Report
 *
 * A trade (or "fill") is created when two opposing orders match in the order book.
 * It records the agreed price, quantity, timestamp, and references to both the
 * buying and selling orders. In industry, every trade triggers:
 *
 * - An ExecutionReport (FIX MsgType=8) sent to both the buyer and seller,
 *   confirming the fill with execID, lastPx, lastQty, cumQty, avgPx.
 * - A trade report to the exchange's post-trade systems for clearing and
 *   settlement (e.g. DTCC in the US, CCASS in Hong Kong, ASX Settlement in AU).
 * - A real-time market data update (last price, last volume) broadcast to all
 *   market data subscribers.
 *
 * Regulators (SEC, FCA, ESMA, SFC) mandate that all trades are reported to a
 * Trade Repository or APA (Approved Publication Arrangement) within seconds.
 * The tradeID here maps to FIX tag 17 (ExecID), and tranSeqNo to tag 34 (MsgSeqNum).
 */
public class Trade implements Serializable, Cloneable {
	public long getTradeID() {
		return tradeID;
	}

	public int getQuantity() {
		return quantity;
	}

	public double getPrice() {
		return price;
	}

	public Date getTranTime() {
		return tranTime;
	}

	public Order getBidOrder() {
		return bidOrder;
	}

	public Order getAskOrder() {
		return askOrder;
	}

	public void setTradeID(long tradeID) {
		this.tradeID = tradeID;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public void setTranTime(Date tranTime) {
		this.tranTime = tranTime;
	}

	public void setBidOrder(Order bidOrder) {
		this.bidOrder = bidOrder;
	}

	public void setAskOrder(Order askOrder) {
		this.askOrder = askOrder;
	}

	protected long tradeID;
	protected int quantity;
	protected double price;
	protected Date tranTime;
	protected Order bidOrder;
	protected Order askOrder;
	protected long tranSeqNo;	
	
	public long getTranSeqNo() {
		return tranSeqNo;
	}

	public void setTranSeqNo(long tranSeqNo) {
		this.tranSeqNo = tranSeqNo;
	}


    public Trade ( long tradeID, int quantity, double price, Order order1, Order order2)
    {
    	this.tradeID = tradeID;
        this.quantity = quantity;
        this.price = price;
        // to make this smart and flexible
        if ( order1.getSide() == Order.SIDE.BID )
        {
        	this.bidOrder = order1;
        	this.askOrder = order2;
        }
        else
        {
        	this.bidOrder = order2;
        	this.askOrder = order1;       	
        }
        tranTime = new Date();
    }
}
