package com.ankur.outbox.service;

import com.ankur.outbox.model.Order;
import com.ankur.outbox.model.OutboxEvent;
import com.ankur.outbox.repository.OrderRepository;
import com.ankur.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(Order order) {
        log.info("Creating order for customer: {}", order.getCustomerName());

        // Save the order
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getId());

        // Create an outbox event in the same transaction
        try {
            String payload = objectMapper.writeValueAsString(savedOrder);
            OutboxEvent outboxEvent = new OutboxEvent(
                "Order",
                savedOrder.getId(),
                "OrderCreated",
                payload
            );

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event created for order ID: {}", savedOrder.getId());
        } catch (Exception e) {
            log.error("Failed to create outbox event", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }

        return savedOrder;
    }
}