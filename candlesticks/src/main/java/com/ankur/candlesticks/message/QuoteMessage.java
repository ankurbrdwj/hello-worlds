package com.ankur.candlesticks.message;

public record QuoteMessage(QuoteData data, String type) {
  public record QuoteData(double price, String isin) {}
}
