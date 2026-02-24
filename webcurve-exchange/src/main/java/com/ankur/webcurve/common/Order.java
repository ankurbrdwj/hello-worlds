package com.ankur.webcurve.common;

import java.io.Serializable;
import java.util.Date;

/**
 * Industry term: Order / Instruction — Exchange-Side Representation
 *
 * Extends BaseOrder with the full lifecycle state that the exchange tracks
 * from submission through to completion. Key additions:
 *
 * - TYPE (LIMIT / MARKET): A LIMIT order rests in the book at a specific price;
 *   a MARKET order executes immediately at the best available price and is
 *   cancelled if unfilled. These map to FIX tag 40 (OrdType): '1'=Market, '2'=Limit.
 *
 * - STATUS: Models the FIX order state machine used globally:
 *   NEW → FILLING (partial fill) → FILLING (full fill, qty=0) or CANCELLED / REJECTED
 *   Maps to FIX tag 39 (OrdStatus).
 *
 * - cumQty / avgPx: Running totals updated on each fill — FIX tags 14 and 6.
 *   These are reported back to the client in every ExecutionReport (FIX MsgType=8).
 *
 * - clOrderId / origClOrderId: Client-assigned IDs (FIX tags 11 and 41) used to
 *   correlate amends and cancels back to the original order at the broker side.
 *
 * Real exchanges (HKEX, ASX, SGX) use this exact state machine in their FIX gateways.
 */
public class Order extends BaseOrder implements Serializable, Cloneable {
	private static final long serialVersionUID = 7337247332181120380L;
	public Order(String code, TYPE type, SIDE side, int quantity, double price, String broker) {
		super(code, side, quantity, price, broker);
		// TODO Auto-generated constructor stub
		this.status = STATUS.NONE;
		this.type = type;
		originalQuantity = quantity;
		createTime = new Date();
		amendTime = new Date();
	}
	
	public Object clone (){
		Order order;
		try {
			order = (Order)super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		return order;
	}

	/**
	 * 
	 */
	public static enum TYPE { LIMIT, MARKET }
	public static enum STATUS { NONE, NEW, FILLING, AMENDED, CANCELLED, REJECTED  }
    
	protected TYPE type; 
	public TYPE getType() {
		return type;
	}
	protected void setType(TYPE type) {
		this.type = type;
	}
	
	protected STATUS status;
	public STATUS getStatus() {
		return status;
	}
	public void setStatus(STATUS status) {
		this.status = status;
	}

	protected long prevOrderID;
	public long getPrevOrderID() {
		return prevOrderID;
	}
	
	public void setPrevOrderID(long prevOrderID)
	{
		this.prevOrderID = prevOrderID;
	}

	protected long parentOrderID;
	public void setParentOrderID(long prevOrderID) {
		this.prevOrderID = prevOrderID;
	}

	public long getParentOrderID() {
		return parentOrderID;
	}
	
	public String getOrigClOrderId() {
		return origClOrderId;
	}

	public String getClOrderId() {
		return clOrderId;
	}

	public void setOrigClOrderId(String origClOrderId) {
		this.origClOrderId = origClOrderId;
	}

	public void setClOrderId(String clOrderId) {
		this.clOrderId = clOrderId;
	}

	protected String origClOrderId;
	protected String clOrderId;
		
	
	
	protected int originalQuantity;
	protected Date createTime;
	protected Date amendTime;
	protected long tranSeqNo;
	
	//get/set
    public int getOriginalQuantity() {
		return originalQuantity;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public Date getAmendTime() {
		return amendTime;
	}
	public long getTranSeqNo() {
		return tranSeqNo;
	}
	public void amendQuantity(int newQty) {
		int delta = newQty - this.getQuantity();
		this.originalQuantity += delta;
		this.setQuantity(newQty);
	}
//	protected void setCreateTime(Date createTime) {
//		this.createTime = createTime;
//	}
	public void setAmendTime(Date amendTime) {
		this.amendTime = amendTime;
	}
	public void setTranSeqNo(long tranSeqNo) {
		this.tranSeqNo = tranSeqNo;
	}
	
	public int getCumQty() {
		return cumQty;
	}

	public double getAvgPrice() {
		return avgPx;
	}

	protected int cumQty;
	protected double avgPx;
	
	public int getLastQty() {
		return lastQty;
	}

	public double getLastPx() {
		return lastPx;
	}

	protected int lastQty;
	protected double lastPx;
	public void calOrder(int quantity, double price)
	{
    	avgPx = avgPx * cumQty /(cumQty + quantity)
			+ price * quantity / (cumQty + quantity);
    	cumQty += quantity;
    	lastQty = quantity;
    	lastPx = price;
	}
	


}
