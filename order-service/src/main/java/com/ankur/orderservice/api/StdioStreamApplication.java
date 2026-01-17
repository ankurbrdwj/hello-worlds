package com.ankur.orderservice.api;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * A command line application which demonstrates how a {@link OrderStreamProcessor} may be used.
 *
 * <p><strong>Important:</strong> do not touch this class! Don't move, rename or otherwise modify
 * it.
 */
public final class StdioStreamApplication {
    private StdioStreamApplication() {
    }

    /**
     * Pipes stdin through the first {@link OrderStreamProcessor} that can {@link ServiceLoader
     * service-loaded}, sending the result to stdout.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.printf("Usage:%n  java -jar application.jar <maxOrders> <maxTime>%n");
            System.exit(1);
        }

        int maxOrders = Integer.parseInt(args[0]);
        Duration maxTime = Duration.parse(args[1]);

        processStdio(maxOrders, maxTime);
    }

    private static void processStdio(int maxOrders, Duration maxTime) throws IOException {
        Iterator<OrderStreamProcessorFactory> factories =
                ServiceLoader.load(OrderStreamProcessorFactory.class).iterator();
        if (!factories.hasNext()) {
            throw new IllegalStateException("No OrderStreamProcessorFactory found");
        }

        OrderStreamProcessorFactory factory = factories.next();
        OrderStreamProcessor processor = factory.createProcessor(maxOrders, maxTime);
        processor.process(System.in, System.out);
    }
}
