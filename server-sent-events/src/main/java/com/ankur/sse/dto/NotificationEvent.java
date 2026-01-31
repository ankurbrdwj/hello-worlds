package com.ankur.sse.dto;

import com.ankur.sse.model.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String userId;
    private EventType type;
    private String payload;
    private LocalDateTime expiresAt;
    private Boolean persistent;

    public static NotificationEvent createDefault(String userId, String message) {
        return NotificationEvent.builder()
                .userId(userId)
                .type(EventType.NOTIFICATION)
                .payload(message)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .persistent(true)
                .build();
    }
}