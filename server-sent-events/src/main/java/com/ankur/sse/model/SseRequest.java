package com.ankur.sse.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sse_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean persistent;

    @Column(nullable = false)
    private Boolean delivered;

    @Column
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (delivered == null) {
            delivered = false;
        }
        if (persistent == null) {
            persistent = true;
        }
    }
}