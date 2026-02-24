package com.ankur.webcurve.exchange;

import com.ankur.webcurve.client.MarketAdaptor;
import com.ankur.webcurve.common.BaseOrder;
import com.ankur.webcurve.common.ExchangeEventListener;
import com.ankur.webcurve.common.Order;
import com.ankur.webcurve.common.Trade;
import com.ankur.webcurve.fix.ExchangeFixGateway;
import com.ankur.webcurve.ui.ExchangeJFrame;
import com.ankur.webcurve.ui.TradeJFrame;
import org.junit.jupiter.api.Test;

public class TestExchange {
    public static String cfgFile = "";


    @Test
    void testShowBook(){
        Exchange exchange = new Exchange();
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 3200, 68.50, "7689", "");
        showBook(exchange.getBook("0005.HK"));
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1200, 68.60, "8675", "");
        showBook(exchange.getBook("0005.HK"));
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1200, 68.50, "8675", "");
        showBook(exchange.getBook("0005.HK"));
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 3600, 68.40, "7689", "");
        showBook(exchange.getBook("0005.HK"));
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1600, 68.70, "8675", "");
        showBook(exchange.getBook("0005.HK"));
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 4000, 68.30, "7689", "");
        showBook(exchange.getBook("0005.HK"));
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 2000, 68.80, "8675", "");
        showBook(exchange.getBook("0005.HK"));
    }


    @Test
    public void testExchangeListeners() {
        Exchange exchange = new Exchange();
        exchange.orderBookListenerKeeper.addExchangeListener(new ExchangeEventListener<OrderBook>(){

            //@Override
            public void onChangeEvent(OrderBook book) {
                showBook(book);
            }

        });

        exchange.tradeListenerKeeper.addExchangeListener(new ExchangeEventListener<Trade>(){

            //@Override
            public void onChangeEvent(Trade trade) {
                showTrade(trade);
            }

        });

        exchange.orderListenerKeeper.addExchangeListener(new ExchangeEventListener<Order>(){

            //@Override
            public void onChangeEvent(Order order) { showOrder(order);
            }

        });
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 3200, 68.50, "7689", "");
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1200, 68.60, "8675", "");
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1200, 68.50, "8675", "");
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 3600, 68.40, "7689", "");
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1600, 68.70, "8675", "");
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 4000, 68.30, "7689", "");
        exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 2000, 68.80, "8675", "");
    }

    @Test
    public void testEventQueue(){
        cfgFile = "";

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ExchangeJFrame frame = new ExchangeJFrame( new Exchange());
                frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                ExchangeFixGateway fixGW = new ExchangeFixGateway(frame.getExchange());
                if (!fixGW.open(cfgFile))
                {
                    System.out.println("Error: Cant open FIX gateway");
                    return;
                }
                frame.setVisible(true);
            }
        });
    }

    @Test
    public void testTradeFrame() {
        cfgFile = "";
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TradeJFrame(cfgFile).setVisible(true);
            }
        });
    }

@Test
void testMarketAdapter(){
    Exchange exchange = new Exchange();
    MarketAdaptor adaptor=new MarketAdaptor(exchange);

    adaptor.subscribeBook("0005.HK", new ExchangeEventListener<OrderBook>(){

        //@Override
        public void onChangeEvent(OrderBook book) {
            showBook(book);
        }

    });

    adaptor.subscribeTrade("0005.HK", new ExchangeEventListener<Trade>(){

        //@Override
        public void onChangeEvent(Trade trade) {
            showTrade(trade);
        }

    });

    adaptor.subscribeOrder("0005.HK", new ExchangeEventListener<Order>(){

        //@Override
        public void onChangeEvent(Order order) {
            showOrder(order);
        }

    });

    exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1200, 68.60, "8675", "");
    exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.ASK, 1200, 68.50, "8675", "");
    exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 3200, 68.30, "7689", "");
    exchange.enterOrder("0005.HK", Order.TYPE.LIMIT, Order.SIDE.BID, 0600, 68.60, "7689", "");
    System.out.println("closing...");
    adaptor.close();
    System.out.println("closed");
}



    public static void showBook(OrderBook book)
    {
        System.out.printf("%n                    %s                      %n", book.getCode());
        System.out.printf("--------------------------------------------%n");
        System.out.printf("         Buy                  Sell          %n");
        System.out.printf("--------------------------------------------%n");
        int i = 0;
        while(i< Math.max(book.getBidOrders().size(),book.getAskOrders().size()))
        {
            if (i < book.getBidOrders().size())
            {
                Order order = book.getBidOrders().get(i);
                System.out.printf("% 10d% 10.3f", order.getQuantity(), order.getPrice());
            }
            else
                System.out.printf("                    ");
            System.out.printf(" | ");
            if (i < book.getAskOrders().size())
            {
                Order order = book.getAskOrders().get(i);
                System.out.printf("%-10.3f% 10d", order.getPrice(), order.getQuantity());
            }
            System.out.printf("%n");
            i++;
        }
        System.out.printf("--------------------------------------------%n%n");
    }

    public static void showTrade(Trade trade)
    {
        System.out.printf("==>Received trade(id %d) update: %d, %.3f%n",
                trade.getTradeID(), trade.getQuantity(), trade.getPrice());
    }

    public static void showOrder(Order order)
    {
        System.out.printf("==>Received Order(id %d) update: %s, %d, %.3f%n",
                order.getOrderID(),
                BaseOrder.sideToString(order.getSide()),
                order.getQuantity(), order.getPrice());
    }
}
