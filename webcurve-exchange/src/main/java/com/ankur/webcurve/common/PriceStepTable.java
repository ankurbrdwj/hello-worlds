package com.ankur.webcurve.common;


import java.math.BigDecimal;
import java.math.RoundingMode;
/**
 * Industry term: Tick Size / Minimum Price Increment (MPI)
 *
 * Every regulated exchange mandates minimum price increments — called "tick sizes"
 * or MPIs — to ensure orderly price formation and prevent quote stuffing (flooding
 * the order book with tiny price variations to slow down competitors).
 *
 * The table implemented here is the actual HKEX (Hong Kong Stock Exchange) tick
 * size regime, in force since the 1990s. For example:
 *   A stock at HK$68.20 falls in the HK$20–100 tier → must be quoted in HK$0.05 steps
 *   A stock at HK$0.10 falls in the HK$0.01–0.25 tier → quoted in HK$0.001 steps
 *
 * Tick size regimes are set by regulators and vary by market:
 * - US:    SEC Rule 612 — minimum $0.01 for stocks priced above $1
 * - EU:    MiFID II tick size regime — varies by average daily turnover band
 * - HKEX:  11-tier table as implemented here
 * - ASX:   Similar tiered table with A$0.001 steps for stocks under A$0.10
 *
 * Any algo trading system operating across multiple venues must implement the
 * correct tick table for each venue. Submitting an off-tick price causes
 * immediate order rejection by the exchange's pre-trade validation layer.
 */
public class PriceStepTable {
	
	/*	HKSE price tick table
	  	0.01	0.25	0.001
		0.25	0.50	0.005
		0.50	10.00	0.010
		10.00	20.00	0.020
		20.00	100.00	0.050
		100.00	200.00	0.100
		200.00	500.00	0.200
		500.00	1000.00	0.500
		1000.00	2000.00	1.000
		2000.00	5000.00	2.000
		5000.00	9995.00	5.000
	 */
	
	static Double HKEXPriceStepTable[][] = { 
		{0.01,	0.25,	0.001},
		{0.25,	0.50,	0.005},
		{0.50,	10.00,	0.010},
		{10.00,	20.00,	0.020},
		{20.00,	100.00,	0.050},
		{100.00,	200.00,	0.100},
		{200.00,	500.00,	0.200},
		{500.00,	1000.00,	0.500},
		{1000.00,	2000.00,	1.000},
		{2000.00,	5000.00,	2.000},
		{5000.00,	9995.00,	5.000}
	};
	
	public BigDecimal getMinPriceStep(String exchange)
	{
		return new BigDecimal(0.001);
	}
	
	public int getMiniPriceStepDecimal(String exchange)
	{
		return 3;
	}
	
	// the reason we need the up parameter is to take care of the boundary cases
	public static BigDecimal getPriceStep(String exchange, String code, Double price, boolean down)
	{
		if (price <= HKEXPriceStepTable[0][0])
			return new BigDecimal(HKEXPriceStepTable[2][0]);
		for (Double item[]: HKEXPriceStepTable)
		{
			if ( (price > item[0] && price < item[1]) || (price == item[1] && down) )
				return new BigDecimal(item[2]);
		}
		return new BigDecimal(HKEXPriceStepTable[2][HKEXPriceStepTable.length-1]);
	}
	
	public double getRoundedPrice(String exchange, String code, Double price)
	{
		BigDecimal bdPrice = new BigDecimal(price);
		BigDecimal bdPriceStep = getPriceStep(exchange, code, price, true);
		bdPrice = bdPrice.divide(bdPriceStep, 0, RoundingMode.HALF_UP).multiply(bdPriceStep);
		return bdPrice.setScale(getMiniPriceStepDecimal(exchange), RoundingMode.DOWN).doubleValue();
		
	}

}
