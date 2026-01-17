package com.ankur.orderservice.service;

import com.ankur.orderservice.api.OrderStreamProcessor;
import com.ankur.orderservice.api.OrderStreamProcessorFactory;
import com.google.auto.service.AutoService;

import java.time.Duration;

/**
 * Factory implementation for creating OrderStreamProcessor instances.
 * This implementation is automatically registered via the ServiceLoader mechanism
 * using the @AutoService annotation.
 */
@AutoService(OrderStreamProcessorFactory.class)
public final class OrderStreamProcessorFactoryImpl implements OrderStreamProcessorFactory {
    
    @Override
    public OrderStreamProcessor createProcessor(int maxOrders, Duration maxTime) {
        if (maxOrders <= 0) {
            throw new IllegalArgumentException("maxOrders must be positive, got: " + maxOrders);
        }
        if (maxTime == null || maxTime.isNegative() || maxTime.isZero()) {
            throw new IllegalArgumentException("maxTime must be positive, got: " + maxTime);
        }
        return new OrderStreamProcessorImpl(maxOrders, maxTime);
    }
}
