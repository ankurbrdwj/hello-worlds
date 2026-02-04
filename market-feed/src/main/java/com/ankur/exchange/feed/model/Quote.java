package com.ankur.exchange.feed.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Quote {
    @JsonProperty("isin")
    private String isin;

    @JsonProperty("price")
    private BigDecimal price;
}