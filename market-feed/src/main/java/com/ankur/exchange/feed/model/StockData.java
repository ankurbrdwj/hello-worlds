package com.ankur.exchange.feed.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockData {
    private String symbol;
    private String name;
}