package com.ankur.price_alert.dto;

import com.ankur.price_alert.model.NotificationChannel;
import com.ankur.price_alert.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private NotificationChannel notificationChannel;
    private boolean isActive;
    private int maxAlerts;
    private int currentAlertCount;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .notificationChannel(user.getNotificationChannel())
                .isActive(user.isActive())
                .maxAlerts(user.getMaxAlerts())
                .currentAlertCount(user.getAlerts() != null ? user.getAlerts().size() : 0)
                .build();
    }
}