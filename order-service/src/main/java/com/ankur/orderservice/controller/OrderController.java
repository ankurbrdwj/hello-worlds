package com.ankur.orderservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping
    public List<Map<String, Object>> getAllOrders() {
        log.info(">>> ORDER-SERVICE: GET /api/orders - Fetching all orders");
        return List.of(
            Map.of("id", 1001, "userId", 1, "product", "Laptop", "amount", 999.99, "status", "DELIVERED"),
            Map.of("id", 1002, "userId", 2, "product", "Phone", "amount", 599.99, "status", "SHIPPED"),
            Map.of("id", 1003, "userId", 1, "product", "Headphones", "amount", 149.99, "status", "PENDING")
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOrderById(@PathVariable Long id) {
        log.info(">>> ORDER-SERVICE: GET /api/orders/{} - Fetching order by ID", id);
        return Map.of(
            "id", id,
            "userId", 1,
            "product", "Product for Order " + id,
            "amount", 99.99,
            "status", "PROCESSING",
            "service", "order-service",
            "port", 8082
        );
    }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> getOrdersByUserId(@PathVariable Long userId) {
        log.info(">>> ORDER-SERVICE: GET /api/orders/user/{} - Fetching orders for user", userId);
        return List.of(
            Map.of("id", 1001, "userId", userId, "product", "Laptop", "amount", 999.99),
            Map.of("id", 1002, "userId", userId, "product", "Mouse", "amount", 29.99)
        );
    }

    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> order) {
        log.info(">>> ORDER-SERVICE: POST /api/orders - Creating order: {}", order);
        return Map.of(
            "id", System.currentTimeMillis(),
            "userId", order.getOrDefault("userId", 0),
            "product", order.getOrDefault("product", "Unknown"),
            "amount", order.getOrDefault("amount", 0),
            "status", "CREATED",
            "service", "order-service"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "order-service",
            "port", 8082
        );
    }
}