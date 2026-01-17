package com.ankur.outbox.scheduler;

import com.ankur.outbox.model.OutboxEvent;
import com.ankur.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * Process pending outbox events every 5 seconds.
     * In a real system, these events would be published to a message broker (Kafka, RabbitMQ, etc.)
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(10);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Simulate publishing to message broker
                publishEvent(event);

                // Mark as processed
                event.markAsProcessed();
                outboxEventRepository.save(event);

                log.info("Successfully processed event ID: {} for {} with event type: {}",
                    event.getId(), event.getAggregateType(), event.getEventType());

            } catch (Exception e) {
                log.error("Failed to process event ID: {}", event.getId(), e);
                event.markAsFailed();
                outboxEventRepository.save(event);
            }
        }
    }

    /**
     * Simulate publishing event to a message broker.
     * In a real system, this would publish to Kafka, RabbitMQ, etc.
     */
    private void publishEvent(OutboxEvent event) {
        log.info("Publishing event to message broker: Event ID={}, Type={}, Aggregate={}:{}",
            event.getId(),
            event.getEventType(),
            event.getAggregateType(),
            event.getAggregateId());

        // Simulate message broker publish
        // In real implementation:
        // kafkaTemplate.send("order-events", event.getPayload());
        // or
        // rabbitTemplate.convertAndSend("order-exchange", "order.created", event.getPayload());

        log.debug("Event payload: {}", event.getPayload());
    }
}