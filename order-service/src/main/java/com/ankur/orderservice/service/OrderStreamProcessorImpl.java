package com.ankur.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ankur.orderservice.api.OrderStreamProcessor;
import com.ankur.orderservice.model.Order;
import com.ankur.orderservice.model.ProcessedDelivery;
import com.ankur.orderservice.util.ObjectMapperFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class OrderStreamProcessorImpl implements OrderStreamProcessor {

  private final int maxOrders;
  private final Duration maxTime;
  private final ObjectMapper mapper = ObjectMapperFactory.createObjectMapper();
  private final OrderProcessor orderProcessor = new OrderProcessor();

  // Streaming optimization: Max deliveries to buffer before flushing
  private static final int MAX_DELIVERY_BUFFER_SIZE = 1000;

  public OrderStreamProcessorImpl(int maxOrders, Duration maxTime) {
    this.maxOrders = maxOrders;
    this.maxTime = maxTime;
  }


  @Override
  public void process(InputStream source, OutputStream sink) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sink, StandardCharsets.UTF_8));

    Map<String, Set<Order>> deliveryMap = new LinkedHashMap<>(); // Preserve insertion order
    int orderCount = 0;
    Instant start = Instant.now();
    boolean firstDelivery = true;

    // Write JSON array opening bracket
    writer.write('[');

    String line;
    while (orderCount < maxOrders && !isTimeoutExceeded(start) && (line = reader.readLine()) != null) {
      Optional<Order> incomingOrder = parseOrder(line);

      if (incomingOrder.isPresent()) {
        orderCount++;
        Order order = incomingOrder.get();

        if (orderProcessor.isRelevantOrder(order)) { //filtering
          deliveryMap                               //grouping
              .computeIfAbsent(order.delivery().deliveryId(), k -> new HashSet<>())
              .add(order);

          // STREAMING: Flush when buffer size exceeds limit
          if (deliveryMap.size() >= MAX_DELIVERY_BUFFER_SIZE) {
            firstDelivery = flushDeliveries(deliveryMap, writer, firstDelivery);
            deliveryMap.clear(); // Free memory!
          }
        }
      }
    }

    // Final flush: Write remaining deliveries
    flushDeliveries(deliveryMap, writer, firstDelivery);

    // Write JSON array closing bracket
    writer.write(']');
    writer.flush(); // Important: Ensure all data is written
  }

  private boolean isTimeoutExceeded(Instant start) {
    return Duration.between(start, Instant.now()).compareTo(maxTime) > 0;
  }

  private Optional<Order> parseOrder(String line) throws IOException {
    if (line.isBlank()) {
      return Optional.empty();
    }
    Order order = mapper.readValue(line, Order.class);
    return Optional.of(order);
  }

  /**
   * Flushes buffered deliveries to the output stream.
   * This enables streaming by writing data incrementally instead of buffering everything.
   *
   * @param deliveryMap current buffer of deliveries to flush
   * @param writer output writer
   * @param isFirstDelivery whether this is the first delivery (affects comma placement)
   * @return false (indicating subsequent deliveries are no longer "first")
   * @throws IOException if writing fails
   */
  private boolean flushDeliveries(Map<String, Set<Order>> deliveryMap, BufferedWriter writer,
                                   boolean isFirstDelivery) throws IOException {
    if (deliveryMap.isEmpty()) {
      return isFirstDelivery;
    }

    List<ProcessedDelivery> deliveries = orderProcessor.getProcessedDeliveries(deliveryMap);

    for (ProcessedDelivery delivery : deliveries) {
      // Add comma before each element except the first
      if (!isFirstDelivery) {
        writer.write(',');
      }

      // Write delivery as JSON (without outer array brackets)
      String json = mapper.writeValueAsString(delivery);
      writer.write(json);

      isFirstDelivery = false;
    }

    writer.flush(); // Flush to output stream immediately (streaming!)
    return isFirstDelivery;
  }

}
