package com.ankur.webcurve.fix;

import com.ankur.webcurve.client.ClientOrder;

import java.util.Hashtable;
public class OrderFixManager {
	protected Hashtable<String, ClientOrder> clientOrders = new Hashtable<String, ClientOrder>();
	protected Hashtable<String, ClientOrder> childOrders = new Hashtable<String, ClientOrder>();

	
}
