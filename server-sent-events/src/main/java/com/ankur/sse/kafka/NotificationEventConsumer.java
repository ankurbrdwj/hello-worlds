package com.ankur.sse.kafka;

import com.ankur.sse.dto.NotificationEvent;
import com.ankur.sse.service.SseNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final SseNotificationService sseNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topic.notifications}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotificationEvent(String message) {
        try {
            log.info("Consumed notification event from Kafka: {}", message);

            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            sseNotificationService.sendNotification(event);

            log.info("Successfully processed notification event for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing notification event: {}", message, e);
        }
    }
}