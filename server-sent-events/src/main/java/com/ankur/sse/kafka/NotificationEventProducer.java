package com.ankur.sse.kafka;

import com.ankur.sse.dto.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.notifications}")
    private String notificationTopic;

    public void sendNotificationEvent(NotificationEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(notificationTopic, event.getUserId(), message);
            log.info("Sent notification event to Kafka for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error sending notification event to Kafka", e);
            throw new RuntimeException("Failed to send notification event", e);
        }
    }
}