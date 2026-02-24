/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * mainJFrame.java
 *
 */

package com.ankur.webcurve.ui;
import com.ankur.webcurve.client.MarketParticipant;
import com.ankur.webcurve.client.MarketReplay;
import com.ankur.webcurve.common.ExchangeEventListener;
import com.ankur.webcurve.common.Order;
import com.ankur.webcurve.common.Trade;
import com.ankur.webcurve.exchange.Exchange;
import com.ankur.webcurve.exchange.OrderBook;
import com.ankur.webcurve.fix.ExchangeFixGateway;
import com.ankur.webcurve.logging.OrderBookEventLogger;
import com.ankur.webcurve.logging.OrderEventLogger;
import com.ankur.webcurve.logging.TradeEventLogger;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * @author dennis_dun_chen@yahoo.com
 */
@Slf4j
public class ExchangeJFrame extends JFrame implements ExchangeEventListener<OrderBook> {
	 private Vector<MarketParticipant> participants = new Vector<MarketParticipant>();
    // user code
    Exchange exchange;
    public Exchange getExchange() {
		return exchange;
	}
	public void setExchange(Exchange exchange) {
		this.exchange = exchange;
	}

	OrderBook currentBook = null;
    Order currentOrder = null;
	ExchangeFixGateway fixGW;
    
    /** Creates new form mainJFrame */
    public ExchangeJFrame(Exchange exchange) {
    	this.exchange = exchange;
        initComponents();
        myInitComponents();
        updateBookView();
    }

    class SelectionListener implements ListSelectionListener
    {
        JTable table;

        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source
        SelectionListener(JTable table) {
            this.table = table;
        }
        public void valueChanged(ListSelectionEvent e) {
            // If cell selection is enabled, both row and column change events are fired
            if (e.getSource() == table.getSelectionModel()
                  && table.getRowSelectionAllowed() && !e.getValueIsAdjusting()) {
                // Column selection changed
                int first = e.getFirstIndex();
                int last = e.getLastIndex();

                //System.out.println("Row: " + first + ", " + last);
                
                // fill the price, quantity,side.. in the edit boxes
                int[] rowIndices = table.getSelectedRows();
                if (rowIndices.length > 0)
                {
                    int i = rowIndices[0];
                    Vector<Order> orders;
                    if (table == lstBid)
                    {
                        orders = currentBook.getBidOrders();
                        // clear the other side selection
                        lstAsk.clearSelection();
                    }
                    else
                    {
                        orders = currentBook.getAskOrders();
                        // clear the other side selection
                        lstBid.clearSelection();

                    }

                    currentOrder = orders.get(i);

                    tbQuantity.setText(new Integer(currentOrder.getQuantity()).toString());
                    tbPrice.setText(new Double(currentOrder.getPrice()).toString());
                    //cbBroker.setSelectedItem(currentOrder.getBroker());
                }

            } else if (e.getSource() == table.getColumnModel().getSelectionModel()
                   && table.getColumnSelectionAllowed() ){
                // Row selection changed
                int first = e.getFirstIndex();
                int last = e.getLastIndex();
                //System.out.println("Col: " + first + ", " + last);
            }

            if (e.getValueIsAdjusting()) {
                // The mouse button has not yet been released
            }
        }
    }

    private void myInitComponents()
    {

        // set frame size and location
    	this.setTitle("Webcurve Simulator - Copyright (c) 2001-2005 to Dennis_d_Chen@yahoo.com");
        setSize(700, 500);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((dim.width-700)/2, (dim.height-500)/2);
        
        InputStream in = MarketReplay.class.getResourceAsStream("Replay Samples.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));       
        String line;
	    try {
			while ((line = br.readLine()) != null)
				jTextArea1.setText(jTextArea1.getText() + line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		jTextArea1.setCaretPosition(0);


        // add order book listener
        SelectionListener bidListener = new SelectionListener(lstBid);
        SelectionListener askListener = new SelectionListener(lstAsk);

        lstBid.getSelectionModel().addListSelectionListener(bidListener);
        lstBid.getColumnModel().getSelectionModel().addListSelectionListener(bidListener);

        lstAsk.getSelectionModel().addListSelectionListener(askListener);
        lstAsk.getColumnModel().getSelectionModel().addListSelectionListener(askListener);

        if(!cbStock.getSelectedItem().toString().equals(""))
            currentBook = exchange.getBook(cbStock.getSelectedItem().toString());

        exchange.orderBookListenerKeeper.addExchangeListener(this);

    }


    public void setBook(String code)
    {
    	currentBook = exchange.getBook(code);
		updateBookView();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new JTabbedPane();
        jPanel1 = new JPanel();
        jPanel5 = new JPanel();
        jLabel1 = new JLabel();
        jLabel6 = new JLabel();
        jLabel4 = new JLabel();
        jLabel5 = new JLabel();
        tbQuantity = new JTextField();
        cbType = new JComboBox();
        tbPrice = new JTextField();
        cbBroker = new JComboBox();
        btBuy = new JButton();
        btSell = new JButton();
        btAmendOrder = new JButton();
        btCancelOrder = new JButton();
        jPanel4 = new JPanel();
        jSplitPane2 = new JSplitPane();
        jSplitPane1 = new JSplitPane();
        jScrollPane1 = new JScrollPane();
        lstBid = new JTable();
        jScrollPane2 = new JScrollPane();
        lstAsk = new JTable();
        jScrollPane3 = new JScrollPane();
        lstTrade = new JTable();
        jPanel3 = new JPanel();
        jLabel3 = new JLabel();
        lbExchange = new JLabel();
        jLabel2 = new JLabel();
        lbVWAP = new JLabel();
        cbStock = new JComboBox();
        cbExchange = new JComboBox();
        tbInstance = new JTextField();
        btBook = new JButton();
        jPanel10 = new JPanel();
        jPanel11 = new JPanel();
        edIndex = new JTextField();
        btIndex = new JButton();
        edClose = new JTextField();
        btClose = new JButton();
        jPanel12 = new JPanel();
        jPanel2 = new JPanel();
        jScrollPane4 = new JScrollPane();
        tbMM = new JTable();
        jPanel7 = new JPanel();
        jLabel7 = new JLabel();
        jLabel8 = new JLabel();
        jLabel9 = new JLabel();
        jLabel11 = new JLabel();
        jLabel13 = new JLabel();
        edStockMM = new JTextField();
        edBasePriceMM = new JTextField();
        edPriceVariantMM = new JTextField();
        edMinQtyMM = new JTextField();
        edMaxQtyMM = new JTextField();
        jLabel12 = new JLabel();
        jLabel14 = new JLabel();
        jLabel15 = new JLabel();
        jLabel16 = new JLabel();
        jLabel17 = new JLabel();
        edStdDevaMM = new JTextField();
        edStdDevaFactorMM = new JTextField();
        edMinIntervalMM = new JTextField();
        edMaxIntervalMM = new JTextField();
        edTradingLengthMM = new JTextField();
        jLabel10 = new JLabel();
        btCreateMM = new JButton();
        btDeleteMM = new JButton();
        btStartMM = new JButton();
        btStopMM = new JButton();
        jPanel6 = new JPanel();
        jPanel8 = new JPanel();
        jScrollPane5 = new JScrollPane();
        jTextArea1 = new JTextArea();
        jPanel9 = new JPanel();
        jLabel18 = new JLabel();
        edAccMR = new JTextField();
        btPlayMR = new JButton();
        btStopMR = new JButton();
        jMenuBar1 = new JMenuBar();
        jMenu1 = new JMenu();
        jMenu2 = new JMenu();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setName("frMain"); // NOI18N
        getContentPane().setLayout(new java.awt.CardLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.GridLayout(3, 0));

        jLabel1.setText("Quantity:");
        jPanel5.add(jLabel1);

        jLabel6.setText("Type:");
        jPanel5.add(jLabel6);

        jLabel4.setText("Price:");
        jPanel5.add(jLabel4);

        jLabel5.setText("Broker ID:");
        jPanel5.add(jLabel5);

        tbQuantity.setText("2000");
        jPanel5.add(tbQuantity);

        cbType.setModel(new DefaultComboBoxModel(new String[] { "LIMIT", "MARKET" }));
        jPanel5.add(cbType);

        tbPrice.setText("68.20");
        jPanel5.add(tbPrice);

        cbBroker.setEditable(true);
        cbBroker.setModel(new DefaultComboBoxModel(new String[] { "1828", "3625", "1481" }));
        jPanel5.add(cbBroker);

        btBuy.setText("Buy");
        btBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btBuyActionPerformed(evt);
            }
        });
        jPanel5.add(btBuy);

        btSell.setText("Sell");
        btSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSellActionPerformed(evt);
            }
        });
        jPanel5.add(btSell);

        btAmendOrder.setText("Amend");
        btAmendOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btAmendOrderActionPerformed(evt);
            }
        });
        jPanel5.add(btAmendOrder);

        btCancelOrder.setText("Cancel");
        btCancelOrder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCancelOrderActionPerformed(evt);
            }
        });
        jPanel5.add(btCancelOrder);

        jPanel1.add(jPanel5, java.awt.BorderLayout.PAGE_END);

        jPanel4.setLayout(new java.awt.BorderLayout());

        jSplitPane2.setDividerLocation(500);

        jSplitPane1.setDividerLocation(250);

        lstBid.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Broker", "Volume", "Price"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(lstBid);

        jSplitPane1.setLeftComponent(jScrollPane1);

        lstAsk.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Price", "Volume", "Broker"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(lstAsk);

        jSplitPane1.setRightComponent(jScrollPane2);

        jSplitPane2.setLeftComponent(jSplitPane1);

        lstTrade.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Trade Price", "Volume"
            }
        ));
        jScrollPane3.setViewportView(lstTrade);

        jSplitPane2.setRightComponent(jScrollPane3);

        jPanel4.add(jSplitPane2, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.GridLayout(2, 0));

        jLabel3.setText("Stock:");
        jPanel3.add(jLabel3);

        lbExchange.setText("Exchange:");
        jPanel3.add(lbExchange);

        jLabel2.setText("Instance:");
        jPanel3.add(jLabel2);

        lbVWAP.setHorizontalAlignment(SwingConstants.LEFT);
        lbVWAP.setText("VWAP:");
        jPanel3.add(lbVWAP);

        cbStock.setEditable(true);
        cbStock.setModel(new DefaultComboBoxModel(new String[] { "0005.HK", "0011.HK", "0857.HK", "3968.HK", "1398.HK", "BHP.AX", "CBA.AX", "ANZ.AX", "WOW.AX" }));
        cbStock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbStockActionPerformed(evt);
            }
        });
        jPanel3.add(cbStock);

        cbExchange.setModel(new DefaultComboBoxModel(new String[] { "HKEX", "ASX", "TWSE", "KRX", "SGX", "JSX" }));
        cbExchange.addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsListener() {
            public void ancestorMoved(java.awt.event.HierarchyEvent evt) {
                cbExchangeAncestorMoved(evt);
            }
            public void ancestorResized(java.awt.event.HierarchyEvent evt) {
            }
        });
        jPanel3.add(cbExchange);

        tbInstance.setText("0");
        jPanel3.add(tbInstance);

        btBook.setText("Detail");
        btBook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btBookActionPerformed(evt);
            }
        });
        jPanel3.add(btBook);

        jPanel1.add(jPanel3, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.addTab("Order Book", jPanel1);

        jPanel10.setLayout(new java.awt.BorderLayout());

        jPanel11.setLayout(new java.awt.GridLayout());
        jPanel11.add(edIndex);

        btIndex.setText("Update Index");
        jPanel11.add(btIndex);
        jPanel11.add(edClose);

        btClose.setText("Update Close Price");
        jPanel11.add(btClose);

        jPanel10.add(jPanel11, java.awt.BorderLayout.NORTH);
        jPanel10.add(jPanel12, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Exchange Data", jPanel10);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jScrollPane4.setPreferredSize(new Dimension(452, 200));

        tbMM.setModel(new DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Stock", "Base Price", "Price Var bp.", "Min Qty", "Max Qty", "Std Deva", "Std D Factor", "Min Interval", "Max Interval", "Length"
            }
        ));
        jScrollPane4.setViewportView(tbMM);

        jPanel2.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jPanel7.setLayout(new java.awt.GridLayout(5, 0));

        jLabel7.setText("Stock");
        jPanel7.add(jLabel7);

        jLabel8.setText("Base Price");
        jPanel7.add(jLabel8);

        jLabel9.setText("Price Variant bp.");
        jPanel7.add(jLabel9);

        jLabel11.setText("Min Qty");
        jPanel7.add(jLabel11);

        jLabel13.setText("Max Qty");
        jPanel7.add(jLabel13);

        edStockMM.setText("0005.HK");
        jPanel7.add(edStockMM);

        edBasePriceMM.setText("68.20");
        jPanel7.add(edBasePriceMM);

        edPriceVariantMM.setText("1000");
        jPanel7.add(edPriceVariantMM);

        edMinQtyMM.setText("1000");
        jPanel7.add(edMinQtyMM);

        edMaxQtyMM.setText("50000");
        jPanel7.add(edMaxQtyMM);

        jLabel12.setText("Std Deva.");
        jPanel7.add(jLabel12);

        jLabel14.setText("Std Factor");
        jPanel7.add(jLabel14);

        jLabel15.setText("Min Interval");
        jPanel7.add(jLabel15);

        jLabel16.setText("Max Interval");
        jPanel7.add(jLabel16);

        jLabel17.setText("Trading Length");
        jPanel7.add(jLabel17);

        edStdDevaMM.setText("1");
        jPanel7.add(edStdDevaMM);

        edStdDevaFactorMM.setText("1");
        jPanel7.add(edStdDevaFactorMM);

        edMinIntervalMM.setText("500");
        jPanel7.add(edMinIntervalMM);

        edMaxIntervalMM.setText("2000");
        jPanel7.add(edMaxIntervalMM);

        edTradingLengthMM.setText("60");
        jPanel7.add(edTradingLengthMM);
        jPanel7.add(jLabel10);

        btCreateMM.setText("Create");
        btCreateMM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCreateMMActionPerformed(evt);
            }
        });
        jPanel7.add(btCreateMM);

        btDeleteMM.setText("Delete");
        btDeleteMM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDeleteMMActionPerformed(evt);
            }
        });
        jPanel7.add(btDeleteMM);

        btStartMM.setText("Start");
        btStartMM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStartMMActionPerformed(evt);
            }
        });
        jPanel7.add(btStartMM);

        btStopMM.setText("Stop");
        btStopMM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStopMMActionPerformed(evt);
            }
        });
        jPanel7.add(btStopMM);

        jPanel2.add(jPanel7, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.addTab("Market Maker", jPanel2);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jPanel8.setLayout(new java.awt.BorderLayout());

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setFont(new java.awt.Font("Courier New", 0, 16));
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setAutoscrolls(false);
        jScrollPane5.setViewportView(jTextArea1);

        jPanel8.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jPanel6.add(jPanel8, java.awt.BorderLayout.CENTER);

        jLabel18.setText("Accelerate/decelerate");
        jPanel9.add(jLabel18);

        edAccMR.setText("1");
        edAccMR.setPreferredSize(new Dimension(50, 25));
        jPanel9.add(edAccMR);

        btPlayMR.setText("Play");
        btPlayMR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btPlayMRActionPerformed(evt);
            }
        });
        jPanel9.add(btPlayMR);

        btStopMR.setText("Stop");
        btStopMR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStopMRActionPerformed(evt);
            }
        });
        jPanel9.add(btStopMR);

        jPanel6.add(jPanel9, java.awt.BorderLayout.SOUTH);

        jTabbedPane1.addTab("Market Replay", jPanel6);

        getContentPane().add(jTabbedPane1, "card2");
        jTabbedPane1.getAccessibleContext().setAccessibleName("orderBookTab");

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Command");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void enterOrder(Order.SIDE side)
    {
        if (currentBook == null)
        {
            //!!! raise some warning here
            return;
        }

        Order.TYPE type;
        if ( cbType.getSelectedItem().toString().equals("MARKET"))
        {
            type = Order.TYPE.MARKET;
            tbPrice.setText("0.0");
        }
        else if ( cbType.getSelectedItem().toString().equals("LIMIT"))
            type = Order.TYPE.LIMIT;
        else
            return;

//        Order order = new Order(cbStock.getSelectedItem().toString(),
//                side, Integer.parseInt(tbQuantity.getText()),
//                Double.parseDouble(tbPrice.getText()), Integer.parseInt(cbBroker.getSelectedItem().toString()));

        exchange.enterOrder(cbStock.getSelectedItem().toString(), type,
        		side, Integer.parseInt(tbQuantity.getText()), Double.parseDouble(tbPrice.getText()),
        				cbBroker.getSelectedItem().toString(), "");
        //updateBookView(currentBook);
    }

    private void btBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btBuyActionPerformed
    enterOrder(Order.SIDE.BID);

}//GEN-LAST:event_btBuyActionPerformed

    private void cbExchangeAncestorMoved(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_cbExchangeAncestorMoved
        // TODO add your handling code here:
}//GEN-LAST:event_cbExchangeAncestorMoved

    private void updateBookView()
    {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	Vector<Order> bidOrders;
            	if (isSum)
            		bidOrders = currentBook.getSumBidOrders();
            	else
            		bidOrders = currentBook.getBidOrders();
           
		        //lstBid.removeAll();
		        DefaultTableModel model = (DefaultTableModel)lstBid.getModel();
		        model.setRowCount(0);
		        for (int i=0; i<bidOrders.size(); i++)
		        {
		            Order order = bidOrders.get(i);
		            model.addRow( new Object[]{order.getBroker(),
		                                       order.getQuantity(),
		                                       order.getPrice()
		                });
		        }
		
		        Vector<Order> askOrders;
		        if(isSum)
		        	askOrders = currentBook.getSumAskOrders();
		        else
		        	askOrders = currentBook.getAskOrders();
		        
		        model = (DefaultTableModel)lstAsk.getModel();
		        model.setRowCount(0);
		        for (int i=0; i<askOrders.size(); i++)
		        {
		            Order order = askOrders.get(i);
		            model.addRow( new Object[]{order.getPrice(),
		                                       order.getQuantity(),
		                                       order.getBroker()
		                });
		        }
		
		        Vector<Trade> trades = currentBook.getTrades();
		        model = (DefaultTableModel)lstTrade.getModel();
		        model.setRowCount(0);
		        for (int i=0; i<trades.size(); i++)
		        {
		            Trade trade = trades.get(i);
		            model.addRow( new Object[]{trade.getPrice(), trade.getQuantity()});
		        }
		
		        //VWAP
		        lbVWAP.setText("VWAP: " + new DecimalFormat("###,###.###").format(currentBook.getVWAP().doubleValue()));
            }
        });
    }

    private boolean isSum = false;
    private void btBookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btBookActionPerformed
        // TODO add your handling code here:
        if (isSum)
        {
        	isSum = false;
        	btBook.setText("Detail");
        	btAmendOrder.setEnabled(true);
        	btCancelOrder.setEnabled(true);
        }
        else
        {
        	isSum = true;
        	btBook.setText("Sum");
        	btAmendOrder.setEnabled(false);
        	btCancelOrder.setEnabled(false);
        }
        updateBookView();
    }//GEN-LAST:event_btBookActionPerformed

    private void cbStockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbStockActionPerformed
        // TODO add your handling code here:
        currentBook = exchange.getBook(cbStock.getSelectedItem().toString());
        updateBookView();
    }//GEN-LAST:event_cbStockActionPerformed

    private void btAmendOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btAmendOrderActionPerformed
        // TODO add your handling code here:
        if (currentOrder == null)
            return;
        exchange.amendOrder(currentOrder.getOrderID(), currentOrder.getCode(), currentOrder.getSide(),
                Integer.parseInt(tbQuantity.getText()), Double.parseDouble(tbPrice.getText()), "");
        //updateBookView(currentBook);
}//GEN-LAST:event_btAmendOrderActionPerformed

    private void cancelOrders(JTable table, Vector<Order> orders)
    {
        if (currentBook == null)
            return;

        int[] rowIndices = table.getSelectedRows();

        Vector<Order> workingOrders = new Vector<Order>();
        for (int i=0; i<rowIndices.length; i++)
        {
            workingOrders.add(orders.get(rowIndices[i]));
        }
        
        for (int i=0; i<workingOrders.size(); i++)
        {
            Order order = workingOrders.get(i);
            if (!exchange.cancelOrder(order.getOrderID(), order.getCode(), order.getSide(), ""))
                System.out.println("cancel order failed at: " + rowIndices[i]);
        }

    }

    private void btCancelOrderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btCancelOrderActionPerformed
        // TODO add your handling code here:
        if (currentBook == null)
            return;

        cancelOrders(lstBid, currentBook.getBidOrders());
        cancelOrders(lstAsk, currentBook.getAskOrders());
        //updateBookView(currentBook);

/*
        if (currentOrder == null)
            return;
        exchange.cancelOrder(currentOrder.getOrderID(), currentOrder.getCode(), currentOrder.getSide());
        updateBookView(currentBook);
 */
}//GEN-LAST:event_btCancelOrderActionPerformed

    private void btSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btSellActionPerformed
        // TODO add your handling code here:
        enterOrder(Order.SIDE.ASK);
    }//GEN-LAST:event_btSellActionPerformed

    
    private void updateMMView()
    {
 
        tbMM.removeAll();
        DefaultTableModel model = (DefaultTableModel)tbMM.getModel();
        model.setRowCount(0);
        for (MarketParticipant mp: participants)
        {
            model.addRow( new Object[]{mp.getStock(),
                                       mp.getBasePrice(),
                                       mp.getPriceVariant(),
                                       mp.getMinQuantity(),
                                       mp.getMaxQuantity(),
                                       mp.getSd(),
                                       mp.getSdFactor(),
                                       mp.getTradingMinInterval(),
                                       mp.getTradingMaxInterval(),
                                       mp.getTradingLength()
                                    
                });
        }
    }
    
    private void btCreateMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btCreateMMActionPerformed
        // TODO add your handling code here:
    	
    	String stock = this.edStockMM.getText();
    	Double price = Double.parseDouble(this.edBasePriceMM.getText());
    	Integer priceVa = Integer.parseInt(this.edPriceVariantMM.getText());
    	Double sd = Double.parseDouble(this.edStdDevaMM.getText());
    	Double sdFactor = Double.parseDouble(this.edStdDevaFactorMM.getText()); 
    	Integer minQty = Integer.parseInt(this.edMinQtyMM.getText());
    	Integer maxQty = Integer.parseInt(this.edMaxQtyMM.getText());
    	Integer minInterval = Integer.parseInt(this.edMinIntervalMM.getText());
    	Integer maxInterval = Integer.parseInt(this.edMaxIntervalMM.getText());
    	Integer tradingLength = Integer.parseInt(this.edTradingLengthMM.getText());
    	
    	MarketParticipant mp = new MarketParticipant( exchange, stock, price,
    			priceVa, // price variant in base points
    			sd, sdFactor, //price change in standard deviation
    			minQty, maxQty, 
    			tradingLength, // time length of trading in minutes
    			minInterval,// min interval in milliseconds
    			maxInterval // max interval in milliseconds
    			);
    	participants.add(mp);
    	updateMMView();
    	mp.start();
    }//GEN-LAST:event_btCreateMMActionPerformed

    private void btDeleteMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btDeleteMMActionPerformed
        // TODO add your handling code here:
    	int[] rows = tbMM.getSelectedRows();
    	if (rows.length < 1)
    		return;    	
    	
    	Vector<MarketParticipant> list = new Vector<MarketParticipant>();
    	for (int row: rows)
    	{
    		MarketParticipant mp = participants.get(row);
    		mp.pause();
    		list.add(mp);
    	}
    	
    	for (MarketParticipant mp: list)
    	{
    		participants.remove(mp);
    	}
    	updateMMView();
    }//GEN-LAST:event_btDeleteMMActionPerformed
    
    private void btStartMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStartMMActionPerformed
        // TODO add your handling code here:
    	int[] rows = tbMM.getSelectedRows();
    	if (rows.length < 1)
    		return;    	
    	
    	for (int row: rows)
    	{
    		MarketParticipant mp = participants.get(row);
    		mp.start();
     	}
    }//GEN-LAST:event_btStartMMActionPerformed

    private void btStopMMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStopMMActionPerformed
        // TODO add your handling code here:
    	int[] rows = tbMM.getSelectedRows();
    	if (rows.length < 1)
    		return;    	
    	
    	for (int row: rows)
    	{
    		MarketParticipant mp = participants.get(row);
    		mp.pause();
     	}
    }//GEN-LAST:event_btStopMMActionPerformed

    private MarketReplay mr = new MarketReplay(exchange, 1);
    private void btPlayMRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btPlayMRActionPerformed
    	mr.stop();
    	JFileChooser fc = new JFileChooser();
  	   	fc.setCurrentDirectory(new File("."));
  	   	if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
  	   		return;
  	   	mr = new MarketReplay(exchange, Integer.parseInt(edAccMR.getText()));
        mr.load(fc.getSelectedFile().getAbsolutePath());
        mr.start();
}//GEN-LAST:event_btPlayMRActionPerformed

    private void btStopMRActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStopMRActionPerformed
        mr.stop();
    }//GEN-LAST:event_btStopMRActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btAmendOrder;
    private JButton btBook;
    private JButton btBuy;
    private JButton btCancelOrder;
    private JButton btClose;
    private JButton btCreateMM;
    private JButton btDeleteMM;
    private JButton btIndex;
    private JButton btPlayMR;
    private JButton btSell;
    private JButton btStartMM;
    private JButton btStopMM;
    private JButton btStopMR;
    private JComboBox cbBroker;
    private JComboBox cbExchange;
    private JComboBox cbStock;
    private JComboBox cbType;
    private JTextField edAccMR;
    private JTextField edBasePriceMM;
    private JTextField edClose;
    private JTextField edIndex;
    private JTextField edMaxIntervalMM;
    private JTextField edMaxQtyMM;
    private JTextField edMinIntervalMM;
    private JTextField edMinQtyMM;
    private JTextField edPriceVariantMM;
    private JTextField edStdDevaFactorMM;
    private JTextField edStdDevaMM;
    private JTextField edStockMM;
    private JTextField edTradingLengthMM;
    private JLabel jLabel1;
    private JLabel jLabel10;
    private JLabel jLabel11;
    private JLabel jLabel12;
    private JLabel jLabel13;
    private JLabel jLabel14;
    private JLabel jLabel15;
    private JLabel jLabel16;
    private JLabel jLabel17;
    private JLabel jLabel18;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JLabel jLabel7;
    private JLabel jLabel8;
    private JLabel jLabel9;
    private JMenu jMenu1;
    private JMenu jMenu2;
    private JMenuBar jMenuBar1;
    private JPanel jPanel1;
    private JPanel jPanel10;
    private JPanel jPanel11;
    private JPanel jPanel12;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel4;
    private JPanel jPanel5;
    private JPanel jPanel6;
    private JPanel jPanel7;
    private JPanel jPanel8;
    private JPanel jPanel9;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JScrollPane jScrollPane4;
    private JScrollPane jScrollPane5;
    private JSplitPane jSplitPane1;
    private JSplitPane jSplitPane2;
    private JTabbedPane jTabbedPane1;
    private JTextArea jTextArea1;
    private JLabel lbExchange;
    private JLabel lbVWAP;
    private JTable lstAsk;
    private JTable lstBid;
    private JTable lstTrade;
    private JTextField tbInstance;
    private JTable tbMM;
    private JTextField tbPrice;
    private JTextField tbQuantity;
    // End of variables declaration//GEN-END:variables

	//@Override
	public void onChangeEvent(OrderBook book) {
		// TODO Auto-generated method stub
		if (book == currentBook)
			updateBookView();
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			Exchange exchange = new Exchange();
			exchange.setName("HKEX");

			exchange.orderListenerKeeper.addExchangeListener(new OrderEventLogger());
			exchange.tradeListenerKeeper.addExchangeListener(new TradeEventLogger());
			exchange.orderBookListenerKeeper.addExchangeListener(new OrderBookEventLogger());

			ExchangeJFrame frame = new ExchangeJFrame(exchange);
			frame.setVisible(true);
		});
	}

}
