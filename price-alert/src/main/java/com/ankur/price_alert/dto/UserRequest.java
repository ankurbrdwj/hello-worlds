package com.ankur.price_alert.dto;

import com.ankur.price_alert.model.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String email;
    private String name;
    private String phone;
    private NotificationChannel notificationChannel;
    private String timezone;
    private Integer maxAlerts;
}