package com.ankur.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents delivery information for an order.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Delivery(
    @JsonProperty("deliveryId") String deliveryId,
    @JsonProperty("address") String address,
    @JsonProperty("timestamp") Long timestamp
) {
}
