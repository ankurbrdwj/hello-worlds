package com.ankur.price_alert.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Alert {
    /*
    Mathematical operators
  You can use the following mathematical operators to set up your alerts:
  Greater than (>).
  Greater than or equal to (>=).
  Less than (<).
  Less than or equal to (<=).
  Equal to (=).
     */
    private double targetPrice;
    private boolean isGreaterThan;
    private boolean isLessThan;
    private boolean isGreaterThanEqualTo;
    private boolean isEqualTo;

    private double lastTradedPrice;
    private double averageTradedPrice;

    // Convenience constructor for benchmarking
    public Alert(double targetPrice, boolean isGreaterThan) {
        this.targetPrice = targetPrice;
        this.isGreaterThan = isGreaterThan;
    }
}
