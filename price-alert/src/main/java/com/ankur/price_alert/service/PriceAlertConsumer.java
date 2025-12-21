package com.ankur.price_alert.service;

import com.ankur.price_alert.model.PriceUpdate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for price updates.
 * Follows Dependency Inversion Principle - depends on AlertEvaluationService interface.
 */
@Service
public class PriceAlertConsumer {

    private final AlertEvaluationService alertEvaluationService;

    public PriceAlertConsumer(AlertEvaluationService alertEvaluationService) {
        this.alertEvaluationService = alertEvaluationService;
    }

    @KafkaListener(topics = "price_updates", groupId = "alert-service")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String symbol = record.key() != null ? record.key() : "TCS";
            double price = Double.parseDouble(record.value());
            System.out.println("📥 Received price update: " + symbol + " = " + price);
            PriceUpdate priceUpdate = new PriceUpdate(symbol, price);
            alertEvaluationService.evaluate(priceUpdate);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to parse message: " + record.value());
        }
    }
}
