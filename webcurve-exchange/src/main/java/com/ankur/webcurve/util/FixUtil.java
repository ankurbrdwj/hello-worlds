package com.ankur.webcurve.util;

import com.ankur.webcurve.client.ClientOrder;
import com.ankur.webcurve.common.BaseOrder;
import com.ankur.webcurve.common.Order;

/**
 * Industry term: FIX Protocol Codec / Message Translator
 *
 * FIX uses compact single-character codes for enums to minimise message size
 * on the wire. These codes are standardised across every FIX-speaking system
 * globally — the same values appear at NYSE, LSE, HKEX, and every major broker:
 *
 * FIX tag 54 (Side):
 *   '1' = Buy,  '2' = Sell,  '5' = Sell Short
 *
 * FIX tag 40 (OrdType):
 *   '1' = Market,  '2' = Limit
 *
 * FixUtil is the codec layer: it translates between the internal domain enums
 * (BaseOrder.SIDE, Order.TYPE) and the FIX wire representation. In production
 * systems, getting this translation wrong has real consequences — accidentally
 * mapping BUY to SELL has caused significant trading losses at live firms.
 *
 * Short selling (Side='5', SELL_SHORT) is treated separately from normal selling
 * because many jurisdictions require disclosure of short positions, and exchanges
 * apply different rules (e.g. the uptick rule in the US, short-sell restrictions
 * during market stress events).
 */
public class FixUtil {
	// Fix utils
	public static char toFixOrderSide(BaseOrder.SIDE side) throws Exception
	{
		if (side == BaseOrder.SIDE.BID)
			return '1';
		else if (side == BaseOrder.SIDE.ASK)
			return '2';
		else
			throw new Exception("toFixOrderSide: unknown side " + side);
	}	

	public static char toFixClientOrderSide(ClientOrder.SIDE side, boolean shortSell) throws Exception
	{
		if (side == ClientOrder.SIDE.BID)
			return '1';
		else if (side == ClientOrder.SIDE.ASK)
		{
			if (shortSell)
				return '5';
			else
				return '2';
		}
		else
			throw new Exception("toFixClientOrderSide: unknown side " + side);
	}	
	
	public static char toFixClientOrderType(ClientOrder.TYPE type) throws Exception
	{
		if (type == ClientOrder.TYPE.MARKET)
			return '1';
		else if (type == ClientOrder.TYPE.LIMIT)
			return '2';
		else
			throw new Exception("toFixClientOrderType: unknown type " + type);
	}	
	
	public static BaseOrder.SIDE fromFixOrderSide(char side) throws Exception
	{
    	if (side == '1')
    		return Order.SIDE.BID;
    	else if (side == '2' || side == '5')
    		return Order.SIDE.ASK;
    	else
			throw new Exception("fromFixOrderSide: unknown side " + side);
	}
}
