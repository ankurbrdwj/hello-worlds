package com.ankur.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an order with product and delivery information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Order(
    @JsonProperty("id") String id,
    @JsonProperty("product") String product,
    @JsonProperty("quantity") int quantity,
    @JsonProperty("delivery") Delivery delivery
) {
}
