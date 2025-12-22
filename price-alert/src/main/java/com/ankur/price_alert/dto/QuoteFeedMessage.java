package com.ankur.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a message from the WebSocket market feed.
 * Example: {"type": "QUOTE", "data": {"isin": "TCS", "price": 416.3398}}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteFeedMessage {
    private String type;
    private QuoteData data;
}