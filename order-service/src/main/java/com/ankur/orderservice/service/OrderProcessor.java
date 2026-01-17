package com.ankur.orderservice.service;

import com.ankur.orderservice.model.Order;
import com.ankur.orderservice.model.ProcessedDelivery;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for processing orders and creating deliveries.
 */
public class OrderProcessor {

    /**
     * Filters orders based on business rules.
     * Currently allows all non-null orders.
     * 
     * @param order the order to check
     * @return true if the order should be processed
     */
    public boolean isRelevantOrder(Order order) {
        // Add filtering logic here based on business requirements
        // For example: filter by product type, quantity, etc.
        return order != null && order.quantity() > 0;
    }

    /**
     * Converts grouped orders into processed deliveries.
     *
     * @param deliveryMap map of delivery IDs to their associated orders
     * @return list of processed deliveries sorted by timestamp (earliest order timestamp in each delivery)
     */
    public List<ProcessedDelivery> getProcessedDeliveries(Map<String, Set<Order>> deliveryMap) {
        // Create intermediate objects with timestamp for sorting
        return deliveryMap.entrySet().stream()
            .map(entry -> {
                String deliveryId = entry.getKey();
                Set<Order> orders = entry.getValue();

                // Get address from first order (all orders in same delivery should have same address)
                String address = orders.stream()
                    .findFirst()
                    .map(order -> order.delivery().address())
                    .orElse("");

                // Get earliest timestamp from all orders in this delivery (for sorting)
                Long timestamp = orders.stream()
                    .map(order -> order.delivery().timestamp())
                    .filter(Objects::nonNull)
                    .min(Long::compareTo)
                    .orElse(0L);

                // Collect all order IDs
                List<String> orderIds = orders.stream()
                    .map(Order::id)
                    .sorted()
                    .collect(Collectors.toList());

                // Calculate total quantity
                int totalQuantity = orders.stream()
                    .mapToInt(Order::quantity)
                    .sum();

                // Create intermediate object with timestamp for sorting
                return new DeliveryWithTimestamp(
                    new ProcessedDelivery(deliveryId, address, orderIds, totalQuantity),
                    timestamp
                );
            })
            .sorted(Comparator.comparing(DeliveryWithTimestamp::timestamp))
            .map(DeliveryWithTimestamp::delivery) // Extract ProcessedDelivery without timestamp
            .collect(Collectors.toList());
    }

    /**
     * Internal wrapper to hold ProcessedDelivery with its timestamp for sorting.
     */
    private record DeliveryWithTimestamp(ProcessedDelivery delivery, Long timestamp) {
    }
}
