package com.ankur.price_alert.dto;

import com.ankur.price_alert.model.NotificationChannel;
import com.ankur.price_alert.model.PriceAlert;
import com.ankur.price_alert.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest {

    @Test
    void fromUser_shouldMapAllFields() {
        // Given
        List<PriceAlert> alerts = new ArrayList<>();
        alerts.add(new PriceAlert());
        alerts.add(new PriceAlert());
        alerts.add(new PriceAlert());

        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .phone("+1234567890")
                .notificationChannel(NotificationChannel.EMAIL)
                .isActive(true)
                .maxAlerts(10)
                .alerts(alerts)
                .build();

        // When
        UserResponse response = UserResponse.fromUser(user);

        // Then
        assertEquals(1L, response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());
        assertEquals("+1234567890", response.getPhone());
        assertEquals(NotificationChannel.EMAIL, response.getNotificationChannel());
        assertTrue(response.isActive());
        assertEquals(10, response.getMaxAlerts());
        assertEquals(3, response.getCurrentAlertCount());
    }

    @Test
    void fromUser_shouldHandleNullAlerts() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .phone("+1234567890")
                .notificationChannel(NotificationChannel.EMAIL)
                .isActive(true)
                .maxAlerts(10)
                .alerts(null)
                .build();

        // When
        UserResponse response = UserResponse.fromUser(user);

        // Then
        assertEquals(0, response.getCurrentAlertCount());
    }

    @Test
    void fromUser_shouldHandleEmptyAlerts() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .notificationChannel(NotificationChannel.SMS)
                .isActive(false)
                .maxAlerts(5)
                .alerts(new ArrayList<>())
                .build();

        // When
        UserResponse response = UserResponse.fromUser(user);

        // Then
        assertEquals(0, response.getCurrentAlertCount());
        assertEquals(NotificationChannel.SMS, response.getNotificationChannel());
        assertFalse(response.isActive());
    }

    @Test
    void userResponse_shouldNotContainTimezoneField() {
        // Given
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .timezone("America/New_York")
                .build();

        // When
        UserResponse response = UserResponse.fromUser(user);

        // Then - verify timezone is not in response by checking class fields
        assertFalse(hasField(UserResponse.class, "timezone"));
    }

    @Test
    void userResponse_shouldNotContainCreatedAtField() {
        // Then - verify createdAt is not in response by checking class fields
        assertFalse(hasField(UserResponse.class, "createdAt"));
    }

    @Test
    void builder_shouldCreateValidResponse() {
        // When
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("john@example.com")
                .name("John Doe")
                .phone("+1234567890")
                .notificationChannel(NotificationChannel.EMAIL)
                .isActive(true)
                .maxAlerts(10)
                .currentAlertCount(3)
                .build();

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());
        assertEquals("+1234567890", response.getPhone());
        assertEquals(NotificationChannel.EMAIL, response.getNotificationChannel());
        assertTrue(response.isActive());
        assertEquals(10, response.getMaxAlerts());
        assertEquals(3, response.getCurrentAlertCount());
    }

    private boolean hasField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}