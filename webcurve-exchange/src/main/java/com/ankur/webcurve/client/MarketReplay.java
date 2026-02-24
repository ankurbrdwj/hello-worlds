package com.ankur.webcurve.client;

import com.ankur.webcurve.common.BaseOrder;
import com.ankur.webcurve.common.Order;
import com.ankur.webcurve.exchange.Exchange;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
/**
 * Industry term: Market Replay / Historical Simulation / Backtesting Engine
 *
 * Market replay is the ability to feed a recorded sequence of historical order
 * events back through the exchange engine as if they were happening live. It is
 * essential infrastructure in every professional trading firm:
 *
 * - Backtesting: running an algo strategy against real historical data to measure
 *   its performance before deploying it with real money
 * - Stress testing: replaying extreme market events (e.g. the 2010 Flash Crash,
 *   COVID March 2020 selloff) to measure system stability under peak load
 * - Debugging: reproducing the exact sequence of market events that triggered a
 *   bug in production — impossible to reproduce without an exact replay
 * - Regulatory audit: exchanges and brokers must retain complete order event logs
 *   and replay them on demand for regulators (MiFID II, Dodd-Frank, SFC)
 *
 * The CSV file format (N=New, A=Amend, C=Cancel with timestamp) mirrors the
 * drop-copy and order event log formats used by exchanges. The timeFactor field
 * mirrors commercial replay tools (e.g. Fidessa Replay, TT Replay) that allow
 * accelerated replay at 10x speed or slowed-down analysis at 0.1x.
 */
@Slf4j
public class MarketReplay implements Runnable {
	class MrElement
	{
		public String tran;
		public String clientOrderId;
		public String stock;
		public BaseOrder.SIDE side;
		public Order.TYPE type;
		public Double price;
		public Integer quantity;
		public Date time;
	}

	private Hashtable<String, Long> idMap = new Hashtable<String, Long>();
	private Vector<MrElement> orders = new Vector<MrElement>(); 
	private Exchange exchange;
	private int timeFactor = 1;
	
	/*
	 * constructs the MarketReplay object
	 * @param exchange the exchange object
	 * @param timeFactor to accelerate or decelerate(negative value) the replay speed
	 */
	public MarketReplay(Exchange exchange, int timeFactor)
	{
		this.exchange = exchange;
		if (timeFactor != 0)
			this.timeFactor = timeFactor;
	}
	/* Samples:
		N,CL001,0005.HK,B,L,68.20,8000,2009.05.22 13:05:17.231
		N,CL002,0005.HK,S,L,68.20,400,2009.05.22 13:05:18.531
		N,CL003,0005.HK,S,M,0.0,2000,2009.05.22 13:05:19:000
		N,CL004,0005.HK,S,L,68.30,6000,2009.05.22 13:05:20.000
		N,CL005,0005.HK,B,L,68.10,4000,2009.05.22 13:05:22.000
		N,CL006,0005.HK,S,L,68.40,2000,2009.05.22 13:05:24.000
		C,CL001,0005.HK,B,L,0,0,2009.05.22 13:05:26.327
		A,CL005,0005.HK,B,L,68.15,0,2009.05.22 13:05:28.423	
	*/
	public void load(String file)
	{
		orders.clear();
		idMap.clear();
		String line;
		try {
		     BufferedReader br = new BufferedReader(new FileReader(file));
		     int lineCount = 0;
		     while ((line = br.readLine()) != null) 
		     {
		    	 lineCount++;
		    	 //skip comments
		    	 if (line.startsWith("#"))
		    		 continue;
		    	 
		    	 String[] tokens = line.split(",");
		    	 if (tokens.length != 8)
		    	 {
		    		 log.error("Insufficient tokens at line: " + lineCount);
		    		 continue;
		    	 }
	    		 MrElement e = new MrElement();
	    		 try
	    		 {
	    			 e.tran = tokens[0];
	    			 e.clientOrderId = tokens[1];
	    			 e.stock = tokens[2];
	    			 if (tokens[3].equalsIgnoreCase("B"))
	    				 e.side = BaseOrder.SIDE.BID;
	    			 else if (tokens[3].equalsIgnoreCase("S"))
	    				 e.side = BaseOrder.SIDE.ASK;
	    			 else
	    				 throw new Exception("Unknown side");
	    			 
	    			 if (tokens[4].equalsIgnoreCase("L"))
	    				 e.type = Order.TYPE.LIMIT;
	    			 else if (tokens[4].equalsIgnoreCase("M"))
	    				 e.type = Order.TYPE.MARKET;
	    			 else
	    				 throw new Exception("Unknown side");
	    			 
	    			 e.price = Double.parseDouble(tokens[5]);
	    			 e.quantity = Integer.parseInt(tokens[6]);
	    			 DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
	    		     e.time = (Date)formatter.parse(tokens[7]);
	    		     orders.add(e);

	    		 }
	    		 catch (Exception ex)
	    		 {
				     log.error("Error at line: " + lineCount + ", " + ex);
	    		 }
		    	 
		     } // end while 
		   } // end try
		catch (IOException e) {
			log.error(e.toString());
		}
		
	}
	
//	@Override
	public void run() {
		if (orders.size()<1)
			return;
		
		playing = true;
		long startTime1 = new Date().getTime();
		long startTime2 = orders.get(0).time.getTime();
		int count = 0;	
	    log.info("Market replay starts at: " + new Date().toString());
		while(playing && count<orders.size())
		{
			MrElement e = orders.get(count);
			long timeToRun;
			if (timeFactor>0)
				timeToRun = startTime1 + (e.time.getTime()-startTime2)/timeFactor - new Date().getTime();
			else
				timeToRun = startTime1 + (e.time.getTime()-startTime2)*-timeFactor - new Date().getTime();

			while (timeToRun > 0)
			{
				if (!playing)
					return;
				try {
					if ( timeToRun > 100 )
					{
						Thread.sleep(100);
						timeToRun -= 100;
					}
					else
					{
						Thread.sleep(timeToRun);
						break;
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					return;
				}
			}
			if (e.tran.equalsIgnoreCase("N"))
			{
				Order order = exchange.enterOrder(e.stock, e.type, e.side, e.quantity, e.price, "Market Replay", e.clientOrderId);
				idMap.put(e.clientOrderId, order.getOrderID());
			}
			else if (e.tran.equalsIgnoreCase("A"))
			{
				Long orderID = idMap.get(e.clientOrderId);
				if (null != orderID)
					exchange.amendOrder(orderID, e.stock, e.side, e.quantity, e.price, e.clientOrderId);
				else
					log.error("AMEND cant find client order id: " + e.clientOrderId);
					
			}
			else if (e.tran.equalsIgnoreCase("C"))
			{
				Long orderID = idMap.get(e.clientOrderId);
				if (null != orderID)
					exchange.cancelOrder(orderID, e.stock, e.side, e.clientOrderId);
				else
					log.error("CANCEL cant find client order id: " + e.clientOrderId);
			}
			count ++;
		}
		playing = false;			
	    log.info("Market replay ends at: " + new Date().toString());
	}
	

	boolean playing = false;
	public void start()
	{
		Thread t = new Thread(this);
		t.start();
	}
	
	public void stop()
	{
		playing = false;
	}
}
