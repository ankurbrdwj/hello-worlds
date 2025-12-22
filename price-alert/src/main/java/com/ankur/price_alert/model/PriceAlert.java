package com.ankur.price_alert.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price_alerts")
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Convenience method to get user email
    public String getUserEmail() {
        return user != null ? user.getEmail() : null;
    }

    @Column(name = "symbol")
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type")
    private AlertType alertType;

    @Column(name = "threshold")
    private double threshold;

    @Column(name = "upper_threshold") // For PRICE_BETWEEN alerts
    private Double upperThreshold;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AlertStatus status = AlertStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_one_time")
    private boolean oneTime = true;

    @Builder.Default
    @Column(name = "max_triggers")
    private int maxTriggers = 1;

    @Builder.Default
    @Column(name = "trigger_count")
    private int triggerCount = 0;

    @Column(name = "last_trigger_price")
    private Double lastTriggerPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

}
