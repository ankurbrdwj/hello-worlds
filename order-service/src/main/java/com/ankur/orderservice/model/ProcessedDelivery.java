package com.ankur.orderservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a processed delivery with aggregated order information.
 */
public record ProcessedDelivery(
    @JsonProperty("deliveryId") String deliveryId,
    @JsonProperty("address") String address,
    @JsonProperty("orderIds") List<String> orderIds,
    @JsonProperty("totalQuantity") int totalQuantity
) {
}
