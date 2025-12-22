package com.ankur.price_alert.controller;

import com.ankur.price_alert.dto.UserRequest;
import com.ankur.price_alert.dto.UserResponse;
import com.ankur.price_alert.model.NotificationChannel;
import com.ankur.price_alert.model.User;
import com.ankur.price_alert.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for user management.
 * Uses custom exception hierarchy - exceptions handled by GlobalExceptionHandler.
 */

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ==================== CREATE ====================

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        User user = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .phone(request.getPhone())
                .notificationChannel(request.getNotificationChannel() != null
                        ? request.getNotificationChannel()
                        : NotificationChannel.EMAIL)
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .maxAlerts(request.getMaxAlerts() != null ? request.getMaxAlerts() : 10)
                .build();

        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(createdUser));
    }

    // ==================== READ ====================

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        User user = userService.getByEmail(email);
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.findAllUsers().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}/alert-slots")
    public ResponseEntity<Map<String, Object>> getAlertSlots(@PathVariable Long id) {
        int remaining = userService.getRemainingAlertSlots(id);
        boolean canCreate = userService.canCreateMoreAlerts(id);

        return ResponseEntity.ok(Map.of(
                "userId", id,
                "remainingSlots", remaining,
                "canCreateMore", canCreate
        ));
    }

    // ==================== UPDATE ====================

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        User updateData = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .notificationChannel(request.getNotificationChannel())
                .timezone(request.getTimezone())
                .maxAlerts(request.getMaxAlerts() != null ? request.getMaxAlerts() : 0)
                .build();

        User updatedUser = userService.updateUser(id, updateData);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/notification-channel")
    public ResponseEntity<UserResponse> updateNotificationChannel(
            @PathVariable Long id,
            @RequestParam NotificationChannel channel) {
        User updatedUser = userService.updateNotificationChannel(id, channel);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/phone")
    public ResponseEntity<UserResponse> updatePhone(
            @PathVariable Long id,
            @RequestParam String phone) {
        User updatedUser = userService.updatePhone(id, phone);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        User updatedUser = userService.activateUser(id);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        User updatedUser = userService.deactivateUser(id);
        return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}