package com.ankur.exchange.feed.util;

import com.ankur.exchange.feed.model.StockData;

import java.util.ArrayList;
import java.util.List;

public class StockList {
    public static final List<StockData> STOCK_LIST = createStockList();

    private static List<StockData> createStockList() {
        List<StockData> stocks = new ArrayList<>();

        // US Stocks
        stocks.add(new StockData("NVDA", "NVIDIA Corporation"));
        stocks.add(new StockData("AAPL", "Apple Inc."));
        stocks.add(new StockData("GOOGL", "Alphabet Inc."));
        stocks.add(new StockData("MSFT", "Microsoft Corporation"));
        stocks.add(new StockData("AMZN", "Amazon.com, Inc."));
        stocks.add(new StockData("META", "Meta Platforms, Inc."));
        stocks.add(new StockData("AVGO", "Broadcom Inc."));
        stocks.add(new StockData("TSLA", "Tesla, Inc."));
        stocks.add(new StockData("NFLX", "Netflix, Inc."));
        stocks.add(new StockData("ASML", "ASML Holding N.V."));

        // Indian Stocks
        stocks.add(new StockData("RELIANCE", "Reliance Industries Ltd"));
        stocks.add(new StockData("HDFCBANK", "HDFC Bank Ltd"));
        stocks.add(new StockData("BHARTIARTL", "Bharti Airtel Ltd"));
        stocks.add(new StockData("TCS", "Tata Consultancy Services Ltd"));
        stocks.add(new StockData("ICICIBANK", "ICICI Bank Ltd"));
        stocks.add(new StockData("SBIN", "State Bank of India"));
        stocks.add(new StockData("INFY", "Infosys Ltd"));
        stocks.add(new StockData("BAJFINANCE", "Bajaj Finance Ltd"));
        stocks.add(new StockData("LT", "Larsen and Toubro Ltd"));
        stocks.add(new StockData("HINDUNILVR", "Hindustan Unilever Ltd"));
        stocks.add(new StockData("MARUTI", "Maruti Suzuki India Ltd"));
        stocks.add(new StockData("ITC", "ITC Ltd"));
        stocks.add(new StockData("HCLTECH", "HCL Technologies Ltd"));
        stocks.add(new StockData("M&M", "Mahindra and Mahindra Ltd"));
        stocks.add(new StockData("KOTAKBANK", "Kotak Mahindra Bank Ltd"));
        stocks.add(new StockData("SUNPHARMA", "Sun Pharmaceutical Industries Ltd"));
        stocks.add(new StockData("AXISBANK", "Axis Bank Ltd"));
        stocks.add(new StockData("ULTRACEMCO", "UltraTech Cement Ltd"));
        stocks.add(new StockData("TITAN", "Titan Company Ltd"));
        stocks.add(new StockData("BAJAJFINSV", "Bajaj Finserv Ltd"));
        stocks.add(new StockData("ADANIPORTS", "Adani Ports and Special Economic Zone Ltd"));
        stocks.add(new StockData("NTPC", "NTPC Ltd"));
        stocks.add(new StockData("ONGC", "Oil and Natural Gas Corporation Ltd"));
        stocks.add(new StockData("BEL", "Bharat Electronics Ltd"));
        stocks.add(new StockData("WIPRO", "Wipro Ltd"));
        stocks.add(new StockData("JSWSTEEL", "JSW Steel Ltd"));
        stocks.add(new StockData("ETERNAL", "Eternal Ltd"));
        stocks.add(new StockData("ASIANPAINT", "Asian Paints Ltd"));
        stocks.add(new StockData("ADANIENT", "Adani Enterprises Ltd"));
        stocks.add(new StockData("BAJAJ-AUTO", "Bajaj Auto Ltd"));
        stocks.add(new StockData("POWERGRID", "Power Grid Corporation of India Ltd"));
        stocks.add(new StockData("NESTLEIND", "Nestle India Ltd"));
        stocks.add(new StockData("COALINDIA", "Coal India Ltd"));
        stocks.add(new StockData("TATASTEEL", "Tata Steel Ltd"));
        stocks.add(new StockData("SBILIFE", "SBI Life Insurance Company Ltd"));
        stocks.add(new StockData("EICHERMOT", "Eicher Motors Ltd"));
        stocks.add(new StockData("INDIGO", "Interglobe Aviation Ltd"));
        stocks.add(new StockData("GRASIM", "Grasim Industries Ltd"));
        stocks.add(new StockData("JIOFIN", "Jio Financial Services Ltd"));
        stocks.add(new StockData("HINDALCO", "Hindalco Industries Ltd"));

        return stocks;
    }
}