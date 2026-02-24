package com.ankur.webcurve.fix;

import com.ankur.webcurve.common.*;
import com.ankur.webcurve.exchange.Exchange;
import com.ankur.webcurve.exchange.OrderBook;
import com.ankur.webcurve.util.FixUtil;
import com.ankur.webcurve.util.PriceUtils;
import lombok.extern.slf4j.Slf4j;
import quickfix.*;
import quickfix.field.*;

import java.util.*;

/**
 * Industry term: FIX Application Handler / Order Management at the Exchange
 *
 * Implements the QuickFIX/J Application interface — the layer that receives
 * decoded FIX messages and translates them into exchange operations. This is
 * the "business logic" of the FIX gateway, sitting above the session/transport layer.
 *
 * FIX message routing handled here mirrors a real exchange FIX engine:
 * - NewOrderSingle (D)             → exchange.enterOrder()
 * - OrderCancelRequest (F)         → exchange.cancelOrder()
 * - OrderCancelReplaceRequest (G)  → exchange.amendOrder()
 * - MarketDataRequest (V)          → subscribe to order book updates
 *
 * Outbound ExecutionReports (8) are sent back to the correct broker session
 * by looking up the broker's SessionID from the order's broker field.
 *
 * In production (e.g. HKEX Orion Central Gateway, ASX Trade), this layer
 * also handles pre-trade risk checks: position limits, fat-finger validation,
 * credit checks, and circuit-breaker enforcement before the order reaches
 * the matching engine.
 */
@Slf4j
public class ExchangeFixManager extends MessageCracker
        implements Application {


    private Exchange exchange;
    private Hashtable<String, SessionID> sessions = new Hashtable<String, SessionID>();
    private Hashtable<String, Order> orders = new Hashtable<String, Order>();

    public ExchangeFixManager(Exchange exchange) throws ConfigError, FieldConvertError {
        this.exchange = exchange;
        exchange.orderListenerKeeper.addExchangeListener(exchangeEventListener);
        exchange.orderBookListenerKeeper.addExchangeListener(orderBookEventListener);
        exchange.tradeListenerKeeper.addExchangeListener(tradeEventListener);
    }

    public void onCreate(SessionID sessionID) {
        log.debug("onCreate: " + sessionID.toString());
        sessions.put(sessionID.getTargetCompID(), sessionID);
    }

    public void onLogon(SessionID sessionID) {
        log.debug("logon: " + sessionID.toString());
        sessions.put(sessionID.getTargetCompID(), sessionID);
    }

    public void onLogout(SessionID sessionID) {
        log.debug("logout: " + sessionID.toString());
        sessions.remove(sessionID.getTargetCompID());
    }

    public void toAdmin(quickfix.Message message, SessionID sessionID) {
    }

    public void toApp(quickfix.Message message, SessionID sessionID) throws DoNotSend {
    }

    public void fromAdmin(quickfix.Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, RejectLogon {
    }

    public void fromApp(quickfix.Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }


    private void sendMessage(SessionID sessionID, Message message) {
        try {
            Session session = Session.lookupSession(sessionID);
            if (session == null) {
                throw new SessionNotFound(sessionID.toString());
            }

            if (!session.isEnabled()) {
                log.info("Client not connected, discard: " + message);
                return;
            }

            DataDictionaryProvider dataDictionaryProvider = session.getDataDictionaryProvider();
            if (dataDictionaryProvider != null) {
                try {
                    dataDictionaryProvider.getApplicationDataDictionary(
                            getApplVerID(session, message)).validate(message, true);
                } catch (Exception e) {
                    log.error("Outgoing message failed validation: " + message);
                    LogUtil.logThrowable(sessionID, e.getMessage(), e);
                    return;
                }
            }

            synchronized (session) {
                session.send(message);
            }
        } catch (SessionNotFound e) {
            log.error(e.getMessage(), e);
        }
    }

    private ApplVerID getApplVerID(Session session, Message message) {
        String beginString = session.getSessionID().getBeginString();
        if (FixVersions.BEGINSTRING_FIXT11.equals(beginString)) {
            return new ApplVerID(ApplVerID.FIX50);
        } else {
            return MessageUtils.toApplVerID(beginString);
        }
    }


    public void onMessage(quickfix.fix42.NewOrderSingle message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        try {
            Side side = message.getSide();
            Order.SIDE orderSide;
            if (side.getValue() == Side.BUY)
                orderSide = Order.SIDE.BID;
            else if (side.getValue() == Side.SELL || side.getValue() == Side.SELL_SHORT)
                orderSide = Order.SIDE.ASK;
            else {
                // !!! send back an rejected ER
                log.warn("Order SIDE not supported");
                return;
            }

            OrdType type = message.getOrdType();
            Order.TYPE orderType;
            if (type.getValue() == OrdType.LIMIT)
                orderType = Order.TYPE.LIMIT;
            else if (type.getValue() == OrdType.MARKET)
                orderType = Order.TYPE.MARKET;
            else {
                // !!! send back an rejected ER
                log.warn("Order TYPE not supported");
                return;
            }

            exchange.enterOrder(message.getSymbol().getValue(), orderType,
                    orderSide, message.getInt(38), // 38 order quantity
                    message.getPrice().getValue(), sessionID.getTargetCompID(),
                    message.getClOrdID().getValue());

        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }

    }

    public void onMessage(quickfix.fix42.OrderCancelReplaceRequest message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

        Order order = orders.get(sessionID.getTargetCompID() + "-" + message.getOrigClOrdID().getValue());
        //construct cancelreject here
        quickfix.fix42.OrderCancelReject reject = new quickfix.fix42.OrderCancelReject(
                new quickfix.field.OrderID("NONE"),
                message.getClOrdID(),
                message.getOrigClOrdID(),
                new quickfix.field.OrdStatus(quickfix.field.OrdStatus.REJECTED),
                new quickfix.field.CxlRejResponseTo(quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REQUEST));

        if (null == order) {
            reject.set(new quickfix.field.Text("Cant find this order(OrigClOrdId): " + message.getOrigClOrdID()));
            sendMessage(sessionID, reject);
            return;
        }

        BaseOrder.SIDE side;
        try {
            side = FixUtil.fromFixOrderSide(message.getSide().getValue());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IncorrectTagValue(54);
        }
        String code = message.getSymbol().getValue();

        int quantity = order.getQuantity();
        // by FIX protocol, quantity change is relative to original order quantity
        if (message.isSetOrderQty()) {
            quantity = message.getInt(38); // order quantity
            if (quantity == 0) {
                reject.set(new quickfix.field.Text("cant amend quantity to 0"));
                sendMessage(sessionID, reject);
                return;
            }

            if (quantity > order.getOriginalQuantity()) {
                reject.set(new quickfix.field.Text("can only amend down quantity"));
                sendMessage(sessionID, reject);
                return;
            }

            int filledQuantity = order.getOriginalQuantity() - order.getQuantity();
            if (quantity <= order.getOriginalQuantity() - order.getQuantity()) {
                reject.set(new quickfix.field.Text("Quantity is less than filled quantity" + filledQuantity));
                sendMessage(sessionID, reject);
                return;
            }

            quantity = order.getQuantity() - (order.getOriginalQuantity() - quantity);
        }

        double price = 0;
        if (message.isSetPrice())
            price = message.getPrice().getValue();
        log.debug("amending order: " + message.getOrigClOrdID().getValue() + ", " + quantity + ", " + price);

        if ((!PriceUtils.Equal(price, 0.0)) && PriceUtils.Equal(price, order.getPrice()) && quantity == order.getQuantity()) {
            log.warn("Price and quantity is the same as before nothing changed, just ack: " + message.getOrigClOrdID().getValue() + ", " + quantity + ", " + price);
            quickfix.fix42.ExecutionReport er;
            try {
                er = createReportFromOrder(order, new OrdStatus(OrdStatus.REPLACED), new ExecType(ExecType.REPLACED));
                sendMessage(sessionID, er);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        if (!exchange.amendOrder(order.getOrderID(), code, side,
                quantity, price, message.getClOrdID().getValue())) {
            log.error("Can't amend this order: " + message.getOrigClOrdID().getValue());
            reject.set(new quickfix.field.Text("Cant amend this order:" + order.getOrderID()));
            sendMessage(sessionID, reject);
        }

    }

    public void onMessage(quickfix.fix42.OrderCancelRequest message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {

        Order order = orders.get(sessionID.getTargetCompID() + "-" + message.getOrigClOrdID().getValue());
        BaseOrder.SIDE side;


        try {
            side = FixUtil.fromFixOrderSide(message.getSide().getValue());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new IncorrectTagValue(54);
        }
        String code = message.getSymbol().getValue();
        log.debug("canceling order: " + message.getOrigClOrdID().getValue() + ", " + code + ", " + side);

        if (order == null || !exchange.cancelOrder(order.getOrderID(), code, side, message.getClOrdID().getValue())) {
            //construct cancelreject here
            quickfix.fix42.OrderCancelReject reject = new quickfix.fix42.OrderCancelReject(
                    new quickfix.field.OrderID("NONE"),
                    message.getClOrdID(),
                    message.getOrigClOrdID(),
                    new quickfix.field.OrdStatus(quickfix.field.OrdStatus.REJECTED),
                    new quickfix.field.CxlRejResponseTo(quickfix.field.CxlRejResponseTo.ORDER_CANCEL_REQUEST));

            reject.set(new quickfix.field.Text("Cant find this order: " + message.getOrigClOrdID()));

            sendMessage(sessionID, reject);
        }

    }

    private void sendTradeUpdate(String mdReqID, SessionID sessionID, Trade trade) {
        quickfix.fix42.MarketDataSnapshotFullRefresh mdfr =
                new quickfix.fix42.MarketDataSnapshotFullRefresh();


        try {
            if (null != mdReqID)
                mdfr.set(new MDReqID(mdReqID));

            mdfr.set(new Symbol(trade.getAskOrder().getCode()));

            //quickfix.field.
            quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries =
                    new quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries();

            noMDEntries.set(new MDEntryType(MDEntryType.TRADE));
            noMDEntries.set(new MDEntryPx(trade.getPrice()));
            noMDEntries.set(new MDEntrySize(trade.getQuantity()));

            mdfr.addGroup(noMDEntries);
            sendMessage(sessionID, mdfr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ExchangeEventListener<OrderBook> orderBookEventListener = new ExchangeEventListener<OrderBook>() {

        @Override
        public void onChangeEvent(OrderBook book) {
            ArrayList<SessionID> sessionIDs = mdSubscription.getSessions(book.getCode());
            if (sessionIDs == null)
                return;
            for (SessionID sessionID : sessionIDs) {
                sendOrderBookUpdate(null, sessionID, book);
            }

        }

    };

    ExchangeEventListener<Trade> tradeEventListener = new ExchangeEventListener<Trade>() {

        @Override
        public void onChangeEvent(Trade trade) {
            ArrayList<SessionID> sessionIDs = mdSubscription.getSessions(trade.getAskOrder().getCode());
            for (SessionID sessionID : sessionIDs) {
                sendTradeUpdate(null, sessionID, trade);
            }

        }

    };

    ExchangeEventListener<Order> exchangeEventListener = new ExchangeEventListener<Order>() {
        //@Override
        public void onChangeEvent(Order order) {
            try {
                SessionID sessionID = sessions.get(order.getBroker());
                if (null == sessionID)
                    return;

                ExecType execType;
                OrdStatus ordStatus;
                int ordQty = order.getOriginalQuantity();
                if (order.getStatus() == Order.STATUS.NEW) {
                    execType = new ExecType(ExecType.NEW);
                    ordStatus = new OrdStatus(OrdStatus.NEW);
                    orders.put(order.getBroker() + "-" + order.getClOrderId(), order);
                } else if (order.getStatus() == Order.STATUS.AMENDED) {
                    execType = new ExecType(ExecType.REPLACED);
                    ordStatus = new OrdStatus(OrdStatus.REPLACED);
                    orders.remove(order.getBroker() + "-" + order.getOrigClOrderId());
                    orders.put(order.getBroker() + "-" + order.getClOrderId(), order);
                } else if (order.getStatus() == Order.STATUS.FILLING) {
                    if (order.getQuantity() == 0) {
                        orders.remove(order.getBroker() + "-" + order.getClOrderId());
                        execType = new ExecType(ExecType.FILL);
                        ordStatus = new OrdStatus(OrdStatus.FILLED);
                    } else {
                        execType = new ExecType(ExecType.PARTIAL_FILL);
                        ordStatus = new OrdStatus(OrdStatus.PARTIALLY_FILLED);
                        orders.put(order.getBroker() + "-" + order.getClOrderId(), order);
                    }
                } else if (order.getStatus() == Order.STATUS.CANCELLED) {
                    orders.remove(order.getBroker() + "-" + order.getClOrderId());
                    execType = new ExecType(ExecType.CANCELED);
                    ordStatus = new OrdStatus(OrdStatus.CANCELED);
                } else if (order.getStatus() == Order.STATUS.REJECTED) {
                    orders.remove(order.getBroker() + "-" + order.getClOrderId());
                    execType = new ExecType(ExecType.REJECTED);
                    ordStatus = new OrdStatus(OrdStatus.REJECTED);
                } else {
                    log.error("Unknow order update type" + order.getStatus());
                    return;
                }

                quickfix.fix42.ExecutionReport er = createReportFromOrder(order, ordStatus, execType);

                sendMessage(sessionID, er);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.toString());
            }
        }
    };

    class MarketDataSubscription {
        ArrayListMap<String, SessionID> symbolSessionMap = new ArrayListMap<String, SessionID>();
        ArrayListMap<String, String> requestSymbolMap = new ArrayListMap<String, String>();
        Map<String, SessionID> requestSessionMap = Collections.synchronizedMap(new HashMap<String, SessionID>());

        void add(String symbol, String requestId, SessionID sessionID) {
            symbolSessionMap.add(symbol, sessionID);
            requestSymbolMap.add(requestId, symbol);
            requestSessionMap.put(requestId, sessionID);
        }

        public ArrayList<SessionID> getSessions(String symbol) {
            return symbolSessionMap.getAll(symbol);
        }


        void remove(String requestID) {
            SessionID sessionID = requestSessionMap.remove(requestID);
            ArrayList<String> symbols = requestSymbolMap.remove(requestID);
            for (String symbol : symbols) {
                symbolSessionMap.remove(symbol, sessionID);
            }
        }
    }

    MarketDataSubscription mdSubscription = new MarketDataSubscription();

    private void sendOrderBookUpdate(String mdReqID, SessionID sessionID, OrderBook book) {
        Vector<Order> bidOrders = book.getSumBidOrders();
        Vector<Order> askOrders = book.getSumAskOrders();

        quickfix.fix42.MarketDataSnapshotFullRefresh mdfr =
                new quickfix.fix42.MarketDataSnapshotFullRefresh();


        try {
            if (null != mdReqID)
                mdfr.set(new MDReqID(mdReqID));

            mdfr.set(new Symbol(book.getCode()));

            //quickfix.field.
            quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries =
                    new quickfix.fix42.MarketDataSnapshotFullRefresh.NoMDEntries();

            mdfr.set(new NoMDEntries());
            for (Order order : bidOrders) {
                noMDEntries.set(new MDEntryType(MDEntryType.BID));
                noMDEntries.set(new MDEntryPx(order.getPrice()));
                noMDEntries.set(new MDEntrySize(order.getQuantity()));
                mdfr.addGroup(noMDEntries);
            }

            for (Order order : askOrders) {
                noMDEntries.set(new MDEntryType(MDEntryType.OFFER));
                noMDEntries.set(new MDEntryPx(order.getPrice()));
                noMDEntries.set(new MDEntrySize(order.getQuantity()));
                mdfr.addGroup(noMDEntries);
            }

//			noMDEntries.set(new MDEntryType(MDEntryType.TRADE));
//			noMDEntries.set(new MDEntryPx(book.getLast()));
//			noMDEntries.set(new MDEntrySize(book.getLastVol()));

//			mdfr.addGroup(noMDEntries);

            sendMessage(sessionID, mdfr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onMessage(quickfix.fix42.MarketDataRequest message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {

        MDReqID mdReqID = message.getMDReqID();

        quickfix.field.NoRelatedSym noRelatedSym = message.get(new NoRelatedSym());
        int symbolCount = noRelatedSym.getValue();

        boolean aggregated = true;
        if (message.isSet(new AggregatedBook())) {
            AggregatedBook aggregatedBook = message.getAggregatedBook();
            if (aggregatedBook.getValue())
                aggregated = true;
            else
                aggregated = false;
        }
        quickfix.fix42.MarketDataRequest.NoRelatedSym groupNoRelatedSym =
                new quickfix.fix42.MarketDataRequest.NoRelatedSym();

        SubscriptionRequestType subType = message.getSubscriptionRequestType();
        if (SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_UPDATE_REQUEST == subType.getValue()) {
            mdSubscription.remove(mdReqID.getValue());
        } else {
            for (int i = 0; i < symbolCount; i++) {
                quickfix.field.Symbol symbolField = new quickfix.field.Symbol();
                message.getGroup(i + 1, groupNoRelatedSym);
                groupNoRelatedSym.get(symbolField);
                log.info("Symbols: " + symbolField.getObject());

                OrderBook book = exchange.getBook(symbolField.getValue());

                sendOrderBookUpdate(mdReqID.getValue(), sessionID, book);
                if (SubscriptionRequestType.SNAPSHOT_UPDATES == subType.getValue())
                    mdSubscription.add(symbolField.getValue(), mdReqID.getValue(), sessionID);

            }
        }
    }


    quickfix.fix42.ExecutionReport createReportFromOrder(Order order, OrdStatus ordStatus, ExecType execType) throws Exception {
        quickfix.fix42.ExecutionReport er = new quickfix.fix42.ExecutionReport(
                new OrderID(Long.toString(order.getOrderID())),
                new ExecID(Long.toString(order.getTranSeqNo())),
                new ExecTransType(ExecTransType.NEW),
                execType,
                ordStatus,
                new Symbol(order.getCode()),
                new Side(FixUtil.toFixOrderSide(order.getSide())),
                new LeavesQty(order.getQuantity()),
                new CumQty(order.getCumQty()),
                new AvgPx(order.getAvgPrice()));

        er.set(new ClOrdID(order.getClOrderId()));
        er.set(new OrderQty(order.getOriginalQuantity()));
        er.set(new Price(order.getPrice()));
        er.set(new LastShares(order.getLastQty()));
        er.set(new LastPx(order.getLastPx()));
        if (order.getOrigClOrderId() != null)
            er.set(new quickfix.field.OrigClOrdID(order.getOrigClOrderId()));

        return er;
    }

}
