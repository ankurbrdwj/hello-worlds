package com.ankur.exchange.feed.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Instrument {
    @JsonProperty("isin")
    private String isin;

    @JsonProperty("description")
    private String description;
}