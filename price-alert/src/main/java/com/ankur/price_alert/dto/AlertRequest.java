package com.ankur.price_alert.dto;

import com.ankur.price_alert.model.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {
    private Long userId;
    private String symbol;
    private AlertType alertType;
    private double threshold;
    private Double upperThreshold;  // For PRICE_BETWEEN alerts
    private boolean oneTime;
    private int maxTriggers;
}