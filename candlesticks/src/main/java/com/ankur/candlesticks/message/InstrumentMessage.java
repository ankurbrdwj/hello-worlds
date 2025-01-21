package com.ankur.candlesticks.message;

public record InstrumentMessage(InstrumentData data, InstrumentType type) {
  public record InstrumentData(String isin, String description) {}
  public enum InstrumentType {ADD, DELETE}

}
