package com.ankur.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox_events")
public class OutboxEvent {

    @Id
    private Long id;

    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String status;

    public OutboxEvent(String aggregateType, Long aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public void markAsProcessed() {
        this.status = "PROCESSED";
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = "FAILED";
    }
}