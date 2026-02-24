package com.ankur.webcurve.exchange;

import com.ankur.webcurve.common.ExchangeEventListener;
import com.ankur.webcurve.common.Order;
import com.ankur.webcurve.common.Trade;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Industry term: Trading Venue / Matching Engine Host
 *
 * In real markets, the Exchange is the central authority — NYSE, NASDAQ, HKEX, LSE.
 * It owns the canonical state of every instrument's order book and is the single
 * source of truth for what orders exist and what trades have occurred.
 *
 * Every broker submitting an order goes through the exchange, which routes it to
 * the correct OrderBook and triggers the matching engine. Real exchanges process
 * millions of orders per second using ultra-low-latency engines (often C++).
 *
 * Here the exchange acts as a facade: it holds one OrderBook per stock code and
 * exposes enterOrder / cancelOrder / amendOrder as the public API. It also owns
 * the three ListenerKeeper event buses (order, trade, book) so that any component
 * (GUI, FIX gateway, logger) can subscribe to exchange events without coupling.
 */
@Slf4j
public class Exchange {
    String name;
    Hashtable<String, OrderBook> books = new Hashtable<String, OrderBook>(); // store order by stock code
    Vector<Trade> trades = new Vector<Trade>();


    /**
     * This property has no effect on the exchange running at the moment. It simply denote the name.
     * @return The name of exchange
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the exchange
     * @param name name of the exchange
     */
    public void setName(String name) {
        this.name = name;
    }
//	ExchangeFixGateway fixGW = new ExchangeFixGateway(this);


//	/**
//	 *  Starts the FIX gateway
//	 */
////	public void startFixGW()
//	{
//		if (!fixGW.open(""))
//			return;
//	}
//
//	/**
//	 * Stops the FIX gateway
//	 */
//	public void closeFixGW()
//	{
//		fixGW.close();
//	}


    /**
     * ListenerKeeper for OrderBook
     */
    public final ListenerKeeper<OrderBook>	orderBookListenerKeeper = new ListenerKeeper<OrderBook>();
    /**
     * ListenerKeeper for Order
     */
    public final ListenerKeeper<Order>	orderListenerKeeper = new ListenerKeeper<Order>();
    /**
     * ListenerKeeper for Trade
     */
    public final ListenerKeeper<Trade>	tradeListenerKeeper = new ListenerKeeper<Trade>();

    private long tranIDSeed = 100;
    protected synchronized long getNextTranID()
    {
        return ++tranIDSeed;
    }

    private long orderIDSeed = 1000;
    protected synchronized long getNextOrderID()
    {
        return ++orderIDSeed;
    }

    protected void touchOrder(Order order)
    {
        order.setAmendTime(new Date());
        order.setTranSeqNo(getNextTranID());
    }

    protected synchronized void addTrades(Vector<Trade> trades)
    {
        if (null==trades)
            return;

        for (int i=0; i<trades.size(); i++)
        {
            Trade trade = trades.get(i);
            trade.setTradeID(this.getNextOrderID());
            trade.setTranSeqNo(this.getNextTranID());
            this.trades.add(trade);
        }
    }

    /**
     * This method retrieves the market depth by code
     * @param code Stock code
     * @return market depth
     */
    public OrderBook getBook(String code)
    {
        OrderBook book = books.get(code);
        if (book == null)
        {
            book = new OrderBook(code);
            books.put(code, book);
        }
        return book;
    }

    // An new order can end up with 3 results
    // 1 - the full order in order book
    // 2 - partial traded, the remains are in order book
    // 3 - fully traded

    /**
     * Enter an order into exchange.
     * An new order can end up with 3 results
     * <li>With full quantity queued in order book
     * <li>Partially traded, the remaining quantity is queued in order book
     * <li>Fully traded, no residual in order book
     * <p>Order.status combines with quantity field can be checked to find out the order results.
     *
     * @param code Stock code
     * @param type only support 2 types at the moment: LIMIT or MARKET
     * @param side Bid or ask(buy or sell)
     * @param quantity Quantity of the order
     * @param price price of the order, only valid for LIMIT type order
     * @param broker broker of this order. This effectively denotes who enters this order.
     * @param clientOrderID Order id assigned by user. Exchange simulator simply puts it in ClOrderID field of
     * the order return
     * @return The order entered.
     */
    public Order enterOrder(String code, Order.TYPE type, Order.SIDE side,
                            int quantity, double price, String broker, String clientOrderID)
    {
        if(quantity ==0 || (type == Order.TYPE.LIMIT && price == 0.0))
            return null;

        Order order = new Order(code, type, side, quantity, price, broker);
        order.setClOrderId(clientOrderID);

        OrderBook book;
        synchronized(books) {
            book = books.get(order.getCode());
            if ( book == null ) //first order
            {
                book = new OrderBook(order.getCode());
                books.put(order.getCode(), book);
            }
        }
        book.enterOrder(order, this, false);
        addTrades(trades);
        orderBookListenerKeeper.updateExchangeListeners(book);
        return order;
    }

    /**
     * Cancel an order in the exchange
     * @param orderID The order ID to be cancelled
     * @param code The stock code of the order
     * @param side the side of the order, bid or ask
     * @param clientOrderID user can assign a new client order id for the cancel action.
     * This is useful for supporting some interfaces such as FIX protocol
     * @return true if successful, false if order can't be found.
     */
    public boolean cancelOrder(long orderID, String code, Order.SIDE side, String clientOrderID)
    {
        OrderBook book = books.get(code);
        if ( book == null ) //first order
            return false;

        boolean result = (book.cancelOrder(orderID, side, this, clientOrderID) != null);
        orderBookListenerKeeper.updateExchangeListeners(book);
        return result;
    }

    /**
     * Amend an order. Only quantity and price are allowed to be amend. Value passed in 0 means no change to this field.
     * Amending up quantity is like entering a new order with the extra up quantity.
     *
     * @param orderID The order ID to be amended
     * @param code The stock code of the order
     * @param side the side of the order, bid or ask
     * @param quantity Quantity of the order amended to. Value of 0 indicates no change.
     * @param price Price of the order amended to. Value of 0.0 indicates no change
     * @param clientOrderID  clientOrderID user can assign a new client order id for the amend action.
     * This is useful for supporting some interfaces such as FIX protocol.
     * @return true if successful, false if amend failed.
     */
    public boolean amendOrder(long orderID, String code, Order.SIDE side,
                              int quantity, double price, String clientOrderID)
    {
        OrderBook book = books.get(code);
        if ( book == null ) //first order
        {
            log.error("Order book is null: " + code);
            return false;
        }

        if (!book.amendOrder(orderID, side, quantity, price, clientOrderID, this))
            return false;
        addTrades(trades);
        orderBookListenerKeeper.updateExchangeListeners(book);
        return true;
    }
}
