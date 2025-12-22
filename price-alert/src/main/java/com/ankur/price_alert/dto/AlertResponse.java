package com.ankur.price_alert.dto;

import com.ankur.price_alert.model.AlertStatus;
import com.ankur.price_alert.model.AlertType;
import com.ankur.price_alert.model.PriceAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String symbol;
    private AlertType alertType;
    private double threshold;
    private Double upperThreshold;
    private AlertStatus status;
    private boolean oneTime;
    private int maxTriggers;
    private int triggerCount;
    private Double lastTriggerPrice;
    private LocalDateTime createdAt;
    private LocalDateTime lastTriggeredAt;

    public static AlertResponse fromAlert(PriceAlert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUser() != null ? alert.getUser().getId() : null)
                .userEmail(alert.getUserEmail())
                .symbol(alert.getSymbol())
                .alertType(alert.getAlertType())
                .threshold(alert.getThreshold())
                .upperThreshold(alert.getUpperThreshold())
                .status(alert.getStatus())
                .oneTime(alert.isOneTime())
                .maxTriggers(alert.getMaxTriggers())
                .triggerCount(alert.getTriggerCount())
                .lastTriggerPrice(alert.getLastTriggerPrice())
                .createdAt(alert.getCreatedAt())
                .lastTriggeredAt(alert.getLastTriggeredAt())
                .build();
    }
}