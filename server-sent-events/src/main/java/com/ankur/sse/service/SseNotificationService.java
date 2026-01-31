package com.ankur.sse.service;

import com.ankur.sse.dto.NotificationEvent;
import com.ankur.sse.model.SseRequest;
import com.ankur.sse.repository.SseRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseNotificationService {

    private final SseRequestRepository sseRequestRepository;
    private final ObjectMapper objectMapper;

    // Store active SSE connections per user
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // Default timeout: 30 minutes
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * Register a new SSE connection for a user
     */
    public SseEmitter registerConnection(String userId) {
        log.info("Registering SSE connection for user: {}", userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // Add emitter to user's connection list
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Handle emitter completion and timeout
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            emitter.complete();
            removeEmitter(userId, emitter);
        });

        emitter.onError(throwable -> {
            log.error("SSE connection error for user: {}", userId, throwable);
            removeEmitter(userId, emitter);
        });

        // Send initial ping
        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("Connection established")
                    .id(String.valueOf(System.currentTimeMillis()))
            );

            // Send any pending persistent events
            sendPendingEvents(userId, emitter);
        } catch (IOException e) {
            log.error("Error sending initial ping for user: {}", userId, e);
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    /**
     * Process and send a notification event to a user
     */
    @Transactional
    public void sendNotification(NotificationEvent event) {
        log.info("Processing notification event for user: {}, type: {}", event.getUserId(), event.getType());

        // Save the event to database
        SseRequest sseRequest = SseRequest.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .payload(event.getPayload())
                .expiresAt(event.getExpiresAt())
                .persistent(event.getPersistent())
                .delivered(false)
                .build();

        sseRequest = sseRequestRepository.save(sseRequest);

        // Try to deliver to active connections
        boolean delivered = deliverToActiveConnections(event.getUserId(), sseRequest);

        if (delivered) {
            sseRequest.setDelivered(true);
            sseRequest.setDeliveredAt(LocalDateTime.now());
            sseRequestRepository.save(sseRequest);
        } else {
            log.warn("No active connection for user: {}. Event stored for later delivery.", event.getUserId());
        }
    }

    /**
     * Deliver event to all active connections for a user
     */
    private boolean deliverToActiveConnections(String userId, SseRequest sseRequest) {
        List<SseEmitter> emitters = userEmitters.get(userId);

        if (emitters == null || emitters.isEmpty()) {
            return false;
        }

        boolean anyDelivered = false;

        for (SseEmitter emitter : emitters) {
            try {
                String eventData = createEventData(sseRequest);
                emitter.send(SseEmitter.event()
                        .name(sseRequest.getType().name())
                        .data(eventData)
                        .id(String.valueOf(sseRequest.getId()))
                );
                anyDelivered = true;
                log.info("Event delivered to user: {}, eventId: {}", userId, sseRequest.getId());
            } catch (IOException e) {
                log.error("Failed to send event to user: {}, eventId: {}", userId, sseRequest.getId(), e);
                removeEmitter(userId, emitter);
            }
        }

        return anyDelivered;
    }

    /**
     * Send pending persistent events when a new connection is established
     */
    private void sendPendingEvents(String userId, SseEmitter emitter) {
        List<SseRequest> pendingEvents = sseRequestRepository
                .findByUserIdAndDeliveredFalseAndPersistentTrueAndExpiresAtAfter(
                        userId,
                        LocalDateTime.now()
                );

        if (!pendingEvents.isEmpty()) {
            log.info("Sending {} pending events to user: {}", pendingEvents.size(), userId);

            for (SseRequest event : pendingEvents) {
                try {
                    String eventData = createEventData(event);
                    emitter.send(SseEmitter.event()
                            .name(event.getType().name())
                            .data(eventData)
                            .id(String.valueOf(event.getId()))
                    );

                    event.setDelivered(true);
                    event.setDeliveredAt(LocalDateTime.now());
                    sseRequestRepository.save(event);

                } catch (IOException e) {
                    log.error("Failed to send pending event to user: {}, eventId: {}", userId, event.getId(), e);
                    break; // Stop sending if connection fails
                }
            }
        }
    }

    /**
     * Create JSON event data
     */
    private String createEventData(SseRequest sseRequest) {
        try {
            Map<String, Object> eventData = Map.of(
                    "id", sseRequest.getId(),
                    "type", sseRequest.getType(),
                    "payload", sseRequest.getPayload(),
                    "timestamp", sseRequest.getCreatedAt().toString(),
                    "expiresAt", sseRequest.getExpiresAt().toString()
            );
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            log.error("Error creating event data", e);
            return "{}";
        }
    }

    /**
     * Remove emitter from user's connection list
     */
    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * Get number of active connections for a user
     */
    public int getActiveConnections(String userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * Cleanup expired events - runs every hour
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredEvents() {
        log.info("Cleaning up expired events");
        sseRequestRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}