package com.ankur.sse.controller;

import com.ankur.sse.dto.NotificationEvent;
import com.ankur.sse.kafka.NotificationEventProducer;
import com.ankur.sse.model.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationEventProducer notificationEventProducer;

    /**
     * Send a notification event via Kafka
     * Example POST body:
     * {
     *   "userId": "user123",
     *   "type": "NOTIFICATION",
     *   "payload": "Hello from notification service!",
     *   "expiresAt": "2026-01-18T10:00:00",
     *   "persistent": true
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendNotification(@RequestBody NotificationEvent event) {
        log.info("Received request to send notification to user: {}", event.getUserId());

        // Set defaults if not provided
        if (event.getExpiresAt() == null) {
            event.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        }
        if (event.getPersistent() == null) {
            event.setPersistent(true);
        }
        if (event.getType() == null) {
            event.setType(EventType.NOTIFICATION);
        }

        notificationEventProducer.sendNotificationEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "userId", event.getUserId(),
                "message", "Notification event sent to Kafka"
        ));
    }

    /**
     * Quick send - simple notification with just userId and message
     * Example: POST /api/notifications/quick?userId=user123&message=Hello!
     */
    @PostMapping("/quick")
    public ResponseEntity<Map<String, String>> quickSend(
            @RequestParam String userId,
            @RequestParam String message) {

        NotificationEvent event = NotificationEvent.createDefault(userId, message);
        notificationEventProducer.sendNotificationEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "userId", userId,
                "message", "Quick notification sent"
        ));
    }

    /**
     * Simulate a biometric authentication request
     */
    @PostMapping("/biometric-auth/{userId}")
    public ResponseEntity<Map<String, String>> sendBiometricAuthRequest(@PathVariable String userId) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .type(EventType.BIOMETRIC_AUTH_REQUEST)
                .payload("{\"action\":\"authenticate\",\"method\":\"fingerprint\",\"transactionId\":\"TXN-" + System.currentTimeMillis() + "\"}")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .persistent(true)
                .build();

        notificationEventProducer.sendNotificationEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "userId", userId,
                "type", "BIOMETRIC_AUTH_REQUEST"
        ));
    }
}