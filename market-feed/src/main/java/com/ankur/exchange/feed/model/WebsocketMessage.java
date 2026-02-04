package com.ankur.exchange.feed.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebsocketMessage<T> {
    @JsonProperty("type")
    private MessageType type;

    @JsonProperty("data")
    private T data;
}