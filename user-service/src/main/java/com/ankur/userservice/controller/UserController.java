package com.ankur.userservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping
    public List<Map<String, Object>> getAllUsers() {
        log.info(">>> USER-SERVICE: GET /api/users - Fetching all users");
        return List.of(
            Map.of("id", 1, "name", "John Doe", "email", "john@example.com"),
            Map.of("id", 2, "name", "Jane Smith", "email", "jane@example.com"),
            Map.of("id", 3, "name", "Bob Wilson", "email", "bob@example.com")
        );
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable Long id) {
        log.info(">>> USER-SERVICE: GET /api/users/{} - Fetching user by ID", id);
        return Map.of(
            "id", id,
            "name", "User " + id,
            "email", "user" + id + "@example.com",
            "service", "user-service",
            "port", 8081
        );
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody Map<String, Object> user) {
        log.info(">>> USER-SERVICE: POST /api/users - Creating user: {}", user);
        return Map.of(
            "id", System.currentTimeMillis(),
            "name", user.getOrDefault("name", "Unknown"),
            "email", user.getOrDefault("email", "unknown@example.com"),
            "status", "created",
            "service", "user-service"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "user-service",
            "port", 8081
        );
    }
}