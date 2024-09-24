package com.ankur.candlesticks.factory;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Candlestick {
    private LocalDateTime openTimestamp;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private LocalDateTime closeTimestamp;

  public Candlestick(LocalDateTime openTimestamp, double openPrice, double highPrice, double lowPrice, double closePrice, LocalDateTime closeTimestamp) {
    this.openTimestamp = openTimestamp;
    this.openPrice = openPrice;
    this.highPrice = highPrice;
    this.lowPrice = lowPrice;
    this.closePrice = closePrice;
    this.closeTimestamp = closeTimestamp;

  }


  }

