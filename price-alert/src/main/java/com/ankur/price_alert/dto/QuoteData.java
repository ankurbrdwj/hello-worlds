package com.ankur.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the inner data of a quote message from WebSocket feed.
 * Example: {"price": 416.3398, "isin": "TCS"}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteData {
    private String isin;
    private double price;
}