package com.ankur.orderservice.api;

import java.time.Duration;

/**
 * An interface for the construction of {@link OrderStreamProcessor}s which limit the amount of data
 * they process.
 *
 * <p><strong>Important:</strong> do not touch this interface! Don't move, rename or otherwise
 * modify it.
 */
public interface OrderStreamProcessorFactory {
    /**
     * Returns a new {@link OrderStreamProcessor} which limits input processing as configured.
     *
     * @param maxOrders the desired and maximum number of orders to be processed.
     * @param maxTime   the maximum amount of time to process orders for; the processor must stop
     *                  reading from its input once this time has elapsed.
     * @return a {@link OrderStreamProcessor} which reads orders from its input stream and writes the
     * result of processing said orders out its output stream.
     */
    OrderStreamProcessor createProcessor(int maxOrders, Duration maxTime);
}
